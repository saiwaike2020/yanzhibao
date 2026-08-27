package com.crm.service;

import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.OwnerType;
import com.crm.common.enums.ResourceStatus;
import com.crm.common.enums.ResourceType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.resource.ResourceResponse;
import com.crm.entity.CompanyMember;
import com.crm.entity.Resource;
import com.crm.entity.ResourceOwner;
import com.crm.entity.SysUser;
import com.crm.event.FileUploadedEvent;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourceRepository;
import com.crm.repository.SysUserRepository;
import com.crm.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务（UC-035，v3.8）。
 *
 * <p>遵循「本地文件系统 + 单体异步处理」架构：
 * <ol>
 *   <li>校验文件非空、类型受支持、存储配额充足；</li>
 *   <li>生成相对抽象标识符 {@code fileKey}（以用户编号 {@code user_no} 命名的独立目录）；</li>
 *   <li>通过 {@link StorageService} 保存物理文件，数据库仅记录 {@code fileKey}（不存绝对路径）；</li>
 *   <li>写入资源记录（状态：待处理 {@code UPLOADED}）并登记所有者；</li>
 *   <li>发布 {@link FileUploadedEvent}，异步监听器执行文件处理，接口立即返回。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    /** 受支持的文件类型（扩展名，小写） */
    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "doc", "docx", "zip");

    private final StorageService storageService;
    private final ResourceRepository resourceRepository;
    private final ResourceOwnerRepository resourceOwnerRepository;
    private final SysUserRepository sysUserRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final ResourceService resourceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 文件上传主流程：保存物理文件 + 记录「待处理」状态 + 发布领域事件后立即返回。
     */
    @Transactional
    public ResourceResponse upload(Long operatorUserId, MultipartFile file,
                                   Long parentResourceId, OwnerType ownerType, Long ownerId) {
        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        // 2. 校验文件类型
        String ext = extensionOf(file.getOriginalFilename());
        if (!SUPPORTED_TYPES.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 3. 归属主体（默认个人归属创建者本人）
        OwnerType finalOwnerType = ownerType != null ? ownerType : OwnerType.USER;
        Long finalOwnerId = ownerId != null ? ownerId : operatorUserId;
        validateOwnership(finalOwnerType, finalOwnerId, operatorUserId);

        // 4. 存储配额校验
        resourceService.validateStorageQuota(finalOwnerType, finalOwnerId, file.getSize());

        // 5. 生成 file_key（用户编号命名的独立目录 + 日期 + 随机文件名）
        SysUser user = sysUserRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        String fileKey = buildFileKey(user.getUserNo(), file.getOriginalFilename());

        // 6. 保存物理文件（通过存储策略抽象，数据库只记录 fileKey）
        try (InputStream in = file.getInputStream()) {
            storageService.save(fileKey, in);
        } catch (IOException e) {
            throw new IllegalStateException("保存上传文件失败", e);
        }

        // 7. 记录资源信息（状态：待处理 UPLOADED）
        Resource resource = new Resource();
        resource.setResourceNo(resourceService.generateResourceNo());
        resource.setName(file.getOriginalFilename());
        resource.setResourceType(ResourceType.FILE);
        resource.setParentResourceId(parentResourceId);
        resource.setCreatorUserId(operatorUserId);
        resource.setFileSize(file.getSize());
        resource.setFileType(ext);
        resource.setFileKey(fileKey);
        resource.setStatus(ResourceStatus.UPLOADED);
        resourceRepository.save(resource);

        // 8. 登记资源所有者
        registerOwner(resource.getResourceId(), finalOwnerType, finalOwnerId, operatorUserId);

        // 9. 发布文件上传领域事件（异步处理）
        eventPublisher.publishEvent(new FileUploadedEvent(
                this, resource.getResourceId(), fileKey,
                file.getOriginalFilename(), ext, file.getSize(), operatorUserId));
        log.info("文件上传完成 resourceId={}, fileKey={}, size={}",
                resource.getResourceId(), fileKey, file.getSize());

        return resourceService.toResponse(resource);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 归属校验：个人资源归属创建者本人；企业资源要求创建者为企业活跃成员 */
    private void validateOwnership(OwnerType ownerType, Long ownerId, Long operatorUserId) {
        if (ownerType == OwnerType.USER) {
            if (!ownerId.equals(operatorUserId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "个人资源所有权必须归属创建者本人");
            }
        } else if (ownerType == OwnerType.COMPANY) {
            CompanyMember member = companyMemberRepository
                    .findByCompanyIdAndUserId(ownerId, operatorUserId)
                    .orElse(null);
            if (member == null || member.getStatus() != MemberStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.NOT_COMPANY_MEMBER);
            }
        }
    }

    /** 登记资源所有者（多所有者模型：起始日期=当前，永久有效） */
    private void registerOwner(Long resourceId, OwnerType ownerType, Long ownerId, Long operatorUserId) {
        ResourceOwner owner = new ResourceOwner();
        owner.setResourceId(resourceId);
        owner.setOwnerType(ownerType);
        owner.setOwnerId(ownerId);
        owner.setValidFrom(LocalDateTime.now());
        owner.setValidUntil(null);
        owner.setGrantedBy(operatorUserId);
        owner.setStatus(1);
        resourceOwnerRepository.save(owner);
    }

    /** 生成 file_key：{userNo}/{yyyy}/{MM}/{dd}/{uuid}.{ext} */
    private String buildFileKey(String userNo, String originalFileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String ext = extensionOf(originalFileName);
        return userNo + "/" + datePath + "/" + random + (ext.isEmpty() ? "" : "." + ext);
    }

    /** 提取扩展名（小写，不含点） */
    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
