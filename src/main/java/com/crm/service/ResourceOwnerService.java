package com.crm.service;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.OwnerType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.resource.GrantOwnershipRequest;
import com.crm.dto.resource.ResourceOwnerResponse;
import com.crm.dto.resource.TransferOwnershipRequest;
import com.crm.dto.resource.UpdateOwnershipValidityRequest;
import com.crm.entity.ResourceOwner;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源所有权服务（多所有者、支持转让、含有效期）。
 *
 * <p>对应需求：v3 文档 6.5.4 所有权管理、UC-023 所有权转让、UC-024 设置所有权有效期。
 */
@Service
@RequiredArgsConstructor
public class ResourceOwnerService {

    private final ResourceOwnerRepository resourceOwnerRepository;
    private final ResourceRepository resourceRepository;
    private final CompanyMemberRepository companyMemberRepository;

    /** 资源所有者列表 */
    public List<ResourceOwnerResponse> listOwners(Long resourceId) {
        return resourceOwnerRepository.findByResourceId(resourceId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 登记新所有者（仅当前有效所有者可操作），含有效期 */
    @Transactional
    public ResourceOwnerResponse addOwner(Long resourceId, Long operatorUserId, GrantOwnershipRequest request) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        validateValidity(request.getValidFrom(), request.getValidUntil());

        if (resourceOwnerRepository.existsByResourceIdAndOwnerTypeAndOwnerId(
                resourceId, request.getOwnerType(), request.getOwnerId())) {
            throw new BusinessException(ErrorCode.OWNER_ALREADY_EXISTS);
        }

        ResourceOwner owner = new ResourceOwner();
        owner.setResourceId(resourceId);
        owner.setOwnerType(request.getOwnerType());
        owner.setOwnerId(request.getOwnerId());
        owner.setValidFrom(request.getValidFrom());
        owner.setValidUntil(request.getValidUntil());
        owner.setGrantedBy(operatorUserId);
        owner.setStatus(1);
        resourceOwnerRepository.save(owner);
        return toResponse(owner);
    }

    /**
     * 所有权转让（UC-023）：为接收方创建所有权，同时撤销操作者（原所有者）的所有权。
     */
    @Transactional
    public ResourceOwnerResponse transferOwnership(Long resourceId, Long operatorUserId, TransferOwnershipRequest request) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        validateValidity(request.getValidFrom(), request.getValidUntil());

        if (resourceOwnerRepository.existsByResourceIdAndOwnerTypeAndOwnerId(
                resourceId, request.getTargetOwnerType(), request.getTargetOwnerId())) {
            throw new BusinessException(ErrorCode.OWNER_ALREADY_EXISTS);
        }

        // 为接收方创建所有权
        ResourceOwner target = new ResourceOwner();
        target.setResourceId(resourceId);
        target.setOwnerType(request.getTargetOwnerType());
        target.setOwnerId(request.getTargetOwnerId());
        target.setValidFrom(request.getValidFrom());
        target.setValidUntil(request.getValidUntil());
        target.setGrantedBy(operatorUserId);
        target.setStatus(1);
        resourceOwnerRepository.save(target);

        // 撤销原所有者（操作者）在该资源上的所有权
        revokeOperatorOwnership(resourceId, operatorUserId);

        return toResponse(target);
    }

    /** 调整所有权有效期（UC-024）：起始可用日期必填，过期时间为空表示一直有效 */
    @Transactional
    public ResourceOwnerResponse updateValidity(Long resourceId, Long ownershipId, Long operatorUserId,
                                                UpdateOwnershipValidityRequest request) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        validateValidity(request.getValidFrom(), request.getValidUntil());

        ResourceOwner owner = resourceOwnerRepository.findByResourceIdAndOwnershipId(resourceId, ownershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_OWNER_NOT_FOUND));
        owner.setValidFrom(request.getValidFrom());
        owner.setValidUntil(request.getValidUntil());
        resourceOwnerRepository.save(owner);
        return toResponse(owner);
    }

    /** 撤销所有权（停用 resource_owners 记录，UC-016） */
    @Transactional
    public void revokeOwnership(Long resourceId, Long operatorUserId, Long ownershipId) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        ResourceOwner owner = resourceOwnerRepository.findByResourceIdAndOwnershipId(resourceId, ownershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_OWNER_NOT_FOUND));
        owner.setStatus(0);
        resourceOwnerRepository.save(owner);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    private void ensureResourceExists(Long resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    /** 校验操作者是否为该资源的有效所有者（直接用户所有者，或企业 OWNER/ADMIN 成员） */
    private void ensureOperatorIsOwner(Long resourceId, Long operatorUserId) {
        boolean isOwner = resourceOwnerRepository.findByResourceId(resourceId).stream()
                .filter(this::isEffective)
                .anyMatch(o -> isOperatorOwnerOf(o, operatorUserId));
        if (!isOwner) {
            throw new BusinessException(ErrorCode.NOT_RESOURCE_OWNER);
        }
    }

    /** 撤销操作者在某资源上的全部有效所有权（转让后原所有者失去所有权） */
    private void revokeOperatorOwnership(Long resourceId, Long operatorUserId) {
        resourceOwnerRepository.findByResourceId(resourceId).stream()
                .filter(this::isEffective)
                .filter(o -> isOperatorOwnerOf(o, operatorUserId))
                .forEach(o -> {
                    o.setStatus(0);
                    resourceOwnerRepository.save(o);
                });
    }

    /** 有效性判定：status=1 且当前时间处于 [validFrom, validUntil] 区间 */
    private boolean isEffective(ResourceOwner owner) {
        if (owner.getStatus() == null || owner.getStatus() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (owner.getValidFrom() == null || owner.getValidFrom().isAfter(now)) {
            return false;
        }
        return owner.getValidUntil() == null || !owner.getValidUntil().isBefore(now);
    }

    /** 判断某所有权记录对应的主体是否为操作者（用户直接拥有，或企业成员角色为 OWNER/ADMIN） */
    private boolean isOperatorOwnerOf(ResourceOwner owner, Long operatorUserId) {
        if (owner.getOwnerType() == OwnerType.USER) {
            return owner.getOwnerId().equals(operatorUserId);
        }
        return companyMemberRepository.findByCompanyIdAndUserId(owner.getOwnerId(), operatorUserId)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE
                        && (m.getRole() == CompanyMemberRole.OWNER || m.getRole() == CompanyMemberRole.ADMIN))
                .orElse(false);
    }

    /** 有效期合法性：过期时间不能早于起始可用日期 */
    private void validateValidity(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validUntil != null && validUntil.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.OWNERSHIP_VALIDITY_INVALID);
        }
    }

    private ResourceOwnerResponse toResponse(ResourceOwner owner) {
        ResourceOwnerResponse response = new ResourceOwnerResponse();
        response.setOwnershipId(owner.getOwnershipId());
        response.setResourceId(owner.getResourceId());
        response.setOwnerType(owner.getOwnerType());
        response.setOwnerId(owner.getOwnerId());
        response.setValidFrom(owner.getValidFrom());
        response.setValidUntil(owner.getValidUntil());
        response.setGrantedBy(owner.getGrantedBy());
        response.setStatus(owner.getStatus());
        response.setCreatedAt(owner.getCreatedAt());
        response.setUpdatedAt(owner.getUpdatedAt());
        return response;
    }
}