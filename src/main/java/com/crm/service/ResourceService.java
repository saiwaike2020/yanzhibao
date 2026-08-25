package com.crm.service;

import com.crm.common.enums.ResourceStatus;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.resource.CreateResourceRequest;
import com.crm.dto.resource.ResourceResponse;
import com.crm.dto.resource.UpdateResourceRequest;
import com.crm.entity.Resource;
import com.crm.entity.ResourceOwner;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourceRepository;
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

    private final ResourceRepository resourceRepository;
    private final ResourceOwnerRepository resourceOwnerRepository;

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
