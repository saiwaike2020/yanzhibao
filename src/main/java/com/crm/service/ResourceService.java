package com.crm.service;

import com.crm.common.enums.OwnerType;
import com.crm.common.enums.ResourceStatus;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.common.enums.MemberStatus;
import com.crm.dto.resource.CreateResourceRequest;
import com.crm.dto.resource.ResourceResponse;
import com.crm.dto.resource.UpdateResourceRequest;
import com.crm.entity.CompanyMember;
import com.crm.entity.Resource;
import com.crm.entity.ResourceOwner;
import com.crm.entity.StorageQuota;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourceRepository;
import com.crm.repository.StorageQuotaRepository;
import com.crm.repository.SystemSettingRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源服务（资料库 / 文件夹 / 文件，统一资源模型）。
 * 所有权关系由 {@link ResourceOwner}（resource_owners）独立维护。
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    /** 个人存储配额参数键 */
    public static final String KEY_QUOTA_PERSONAL = "storage.quota.personal";
    /** 企业存储配额参数键 */
    public static final String KEY_QUOTA_COMPANY = "storage.quota.company";
    /** 个人存储默认上限：200MB */
    public static final long DEFAULT_PERSONAL_QUOTA_BYTES = 200L * 1024 * 1024;
    /** 企业存储默认上限：10GB */
    public static final long DEFAULT_COMPANY_QUOTA_BYTES = 10L * 1024 * 1024 * 1024;

    private final ResourceRepository resourceRepository;
    private final ResourceOwnerRepository resourceOwnerRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final StorageQuotaRepository storageQuotaRepository;
    private final CompanyMemberRepository companyMemberRepository;

    /**
     * 创建资源（UC-010）：创建资源并登记初始所有者。
     * 初始所有者起始可用日期=创建时间，永久有效（validUntil 为空）。
     */
    @Transactional
    public ResourceResponse createResource(Long operatorUserId, CreateResourceRequest request) {
        // 父级资源存在性校验（资料库无父级）
        if (request.getParentResourceId() != null && !resourceRepository.existsById(request.getParentResourceId())) {
            throw new BusinessException(ErrorCode.PARENT_RESOURCE_INVALID);
        }
        // TODO: 父级写权限校验（权限判定实现后补充）

        // 所有权归属校验（v3.5）：个人资源归属创建者本人；企业资源要求创建者为企业活跃成员
        if (request.getOwnerType() == OwnerType.USER) {
            if (!request.getOwnerId().equals(operatorUserId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "个人资源所有权必须归属创建者本人");
            }
        } else if (request.getOwnerType() == OwnerType.COMPANY) {
            CompanyMember member = companyMemberRepository
                    .findByCompanyIdAndUserId(request.getOwnerId(), operatorUserId)
                    .orElse(null);
            if (member == null || member.getStatus() != MemberStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.NOT_COMPANY_MEMBER);
            }
        }

        // 存储配额校验（UC-029）：文件类资源占用存储，超出归属主体配额则拒绝
        if (request.getFileSize() != null && request.getFileSize() > 0) {
            checkStorageQuota(request.getOwnerType(), request.getOwnerId(), request.getFileSize());
        }

        Resource resource = new Resource();
        resource.setResourceNo(generateResourceNo());
        resource.setName(request.getName());
        resource.setResourceType(request.getResourceType());
        resource.setParentResourceId(request.getParentResourceId());
        resource.setCreatorUserId(operatorUserId);
        resource.setFileSize(request.getFileSize());
        resource.setFileType(request.getFileType());
        resource.setFilePath(request.getFilePath());
        resource.setStatus(ResourceStatus.ACTIVE);
        resourceRepository.save(resource);

        // 登记初始所有者（多所有者模型）：创建者（或个人/企业主体）成为资源所有者
        ResourceOwner owner = new ResourceOwner();
        owner.setResourceId(resource.getResourceId());
        owner.setOwnerType(request.getOwnerType());
        owner.setOwnerId(request.getOwnerId());
        owner.setValidFrom(LocalDateTime.now());
        owner.setValidUntil(null); // 永久有效
        owner.setGrantedBy(operatorUserId);
        owner.setStatus(1);
        resourceOwnerRepository.save(owner);

        return toResponse(resource);
    }

    /** 资源树（按权限过滤） */
    public List<ResourceResponse> getResourceTree(Long rootResourceId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 资源详情 */
    public ResourceResponse getResource(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(resource);
    }

    /** 更新资源（重命名、移动，需父级写权限） */
    public ResourceResponse updateResource(Long resourceId, UpdateResourceRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 删除 / 归档资源（需 OWNER 权限） */
    public void deleteResource(Long resourceId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 生成资源编号：RES + 时间戳(毫秒) + 3 位随机数 */
    private String generateResourceNo() {
        for (int i = 0; i < 10; i++) {
            String resourceNo = "RES" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (!resourceRepository.existsByResourceNo(resourceNo)) {
                return resourceNo;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "资源编号生成失败，请重试");
    }

    /**
     * 存储配额校验（UC-029）：已用存储 + 新文件大小 ≤ 归属主体配额。
     * 配额来源为系统参数 storage.quota.personal / storage.quota.company，可被系统管理员调节（UC-030）。
     */
    private void checkStorageQuota(OwnerType ownerType, Long ownerId, Long fileSize) {
        long used = resourceRepository.sumFileSizeByOwner(ownerType, ownerId, ResourceStatus.ACTIVE);
        long quota = getStorageQuota(ownerType, ownerId);
        if (used + fileSize > quota) {
            throw new BusinessException(ErrorCode.STORAGE_QUOTA_EXCEEDED);
        }
    }

    /** 读取归属主体的存储配额（字节）：个体专属配额优先，未设置则回退全局默认配额（UC-031） */
    private long getStorageQuota(OwnerType ownerType, Long ownerId) {
        // 1. 个体专属配额优先
        StorageQuota specific = storageQuotaRepository
                .findByQuotaTypeAndSubjectId(ownerType, ownerId).orElse(null);
        if (specific != null && specific.getQuotaBytes() != null && specific.getQuotaBytes() > 0) {
            return specific.getQuotaBytes();
        }
        // 2. 回退全局默认配额（system_settings 或内置默认值）
        String key = ownerType == OwnerType.USER ? KEY_QUOTA_PERSONAL : KEY_QUOTA_COMPANY;
        long defaultValue = ownerType == OwnerType.USER
                ? DEFAULT_PERSONAL_QUOTA_BYTES : DEFAULT_COMPANY_QUOTA_BYTES;
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> parseQuota(s.getSettingValue(), defaultValue))
                .orElse(defaultValue);
    }

    /** 解析配额字符串，非法或非正数时回退默认值 */
    private long parseQuota(String value, long defaultValue) {
        try {
            long quota = Long.parseLong(value.trim());
            return quota > 0 ? quota : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private ResourceResponse toResponse(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setResourceId(resource.getResourceId());
        response.setResourceNo(resource.getResourceNo());
        response.setName(resource.getName());
        response.setResourceType(resource.getResourceType());
        response.setParentResourceId(resource.getParentResourceId());
        response.setCreatorUserId(resource.getCreatorUserId());
        response.setFileSize(resource.getFileSize());
        response.setFileType(resource.getFileType());
        response.setFilePath(resource.getFilePath());
        response.setStatus(resource.getStatus());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        return response;
    }
}
