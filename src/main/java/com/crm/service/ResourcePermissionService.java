package com.crm.service;

import com.crm.common.enums.AccessLevel;
import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.GranteeType;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.OwnerType;
import com.crm.common.enums.PermissionLevel;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.resource.GrantPermissionRequest;
import com.crm.dto.resource.ResourcePermissionResponse;
import com.crm.dto.resource.UpdateOriginalOwnerPermissionRequest;
import com.crm.dto.resource.UpdatePermissionRequest;
import com.crm.entity.CompanyMember;
import com.crm.entity.ResourceOwner;
import com.crm.entity.ResourcePermission;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.GroupRepository;
import com.crm.repository.ResourceOwnerRepository;
import com.crm.repository.ResourcePermissionRepository;
import com.crm.repository.ResourceRepository;
import com.crm.repository.SysUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源权限服务（授权主体支持分组与用户，权限级别 READ / WRITE / OWNER）。
 *
 * <p>设计文档 6.4 / 需求 2.3：**资源的读、写、所有权均通过起始可用日期（validFrom）与
 * 过期时间（validUntil）控制有效期**——起始可用日期必填，未设置过期时间则一直有效；
 * 权限判定仅统计当前时间处于 [validFrom, validUntil] 区间内的授权记录。
 */
@Service
@RequiredArgsConstructor
public class ResourcePermissionService {

    private final ResourcePermissionRepository resourcePermissionRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceOwnerRepository resourceOwnerRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final GroupRepository groupRepository;
    private final SysUserRepository sysUserRepository;

    /** 资源权限列表（含有效期） */
    public List<ResourcePermissionResponse> listPermissions(Long resourceId) {
        ensureResourceExists(resourceId);
        return resourcePermissionRepository.findByResourceId(resourceId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 分配资源权限给分组 (UC-011) */
    @Transactional
    public void grantToGroup(Long resourceId, Long operatorUserId, GrantPermissionRequest request) {
        grantPermission(resourceId, operatorUserId, GranteeType.GROUP, request);
    }

    /** 分配资源权限给用户 (UC-012) */
    @Transactional
    public void grantToUser(Long resourceId, Long operatorUserId, GrantPermissionRequest request) {
        grantPermission(resourceId, operatorUserId, GranteeType.USER, request);
    }

    /** 授权统一入口：校验操作者 / 授权主体 / 有效期 / 重复授权，保存含有效期的授权记录 */
    private void grantPermission(Long resourceId, Long operatorUserId, GranteeType expectedType,
                                 GrantPermissionRequest request) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        validateValidity(request.getValidFrom(), request.getValidUntil());
        validateGrantee(request.getGranteeType(), request.getGranteeId());

        if (request.getGranteeType() != expectedType) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "授权主体类型与接口不匹配");
        }
        if (resourcePermissionRepository.existsByResourceIdAndGranteeTypeAndGranteeId(
                resourceId, request.getGranteeType(), request.getGranteeId())) {
            throw new BusinessException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        ResourcePermission permission = new ResourcePermission();
        permission.setResourceId(resourceId);
        permission.setGranteeType(request.getGranteeType());
        permission.setGranteeId(request.getGranteeId());
        permission.setPermissionLevel(request.getPermissionLevel());
        permission.setValidFrom(request.getValidFrom());
        permission.setValidUntil(request.getValidUntil());
        permission.setGrantedBy(operatorUserId);
        resourcePermissionRepository.save(permission);
    }

    /** 修改权限级别 / 有效期 */
    @Transactional
    public void updatePermission(Long resourceId, Long permissionId, UpdatePermissionRequest request) {
        ensureResourceExists(resourceId);
        validateValidity(request.getValidFrom(), request.getValidUntil());
        ResourcePermission permission = resourcePermissionRepository
                .findByResourceIdAndPermissionId(resourceId, permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));
        permission.setPermissionLevel(request.getPermissionLevel());
        permission.setValidFrom(request.getValidFrom());
        permission.setValidUntil(request.getValidUntil());
        resourcePermissionRepository.save(permission);
    }

    /** 撤销资源权限 (UC-016) */
    @Transactional
    public void revokePermission(Long resourceId, Long permissionId) {
        ensureResourceExists(resourceId);
        ResourcePermission permission = resourcePermissionRepository
                .findByResourceIdAndPermissionId(resourceId, permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND));
        resourcePermissionRepository.delete(permission);
    }

    /**
     * 企业管理员调整原所有者访问权限（UC-034，v3.6）。
     *
     * <p>个人用户将所有权分享（转让）给企业后，企业管理员可将原用户的访问权限调整为：
     * NONE（撤销全部授权）/ READ / WRITE（upsert 对应授权，永久有效）。
     */
    @Transactional
    public void setOriginalOwnerPermission(Long resourceId, Long operatorUserId,
                                           Long originalOwnerUserId, AccessLevel level) {
        ensureResourceExists(resourceId);
        ensureOperatorIsOwner(resourceId, operatorUserId);
        ensureOriginalOwner(resourceId, originalOwnerUserId);

        if (level == AccessLevel.NONE) {
            // 无权：撤销原用户在该资源上的全部授权
            resourcePermissionRepository.deleteByResourceIdAndGranteeTypeAndGranteeId(
                    resourceId, GranteeType.USER, originalOwnerUserId);
            return;
        }

        // 只读 / 可写：upsert 授权记录（起始日期=当前时间，永久有效）
        PermissionLevel permLevel = level == AccessLevel.READ ? PermissionLevel.READ : PermissionLevel.WRITE;
        ResourcePermission permission = resourcePermissionRepository
                .findByResourceIdAndGranteeTypeAndGranteeId(resourceId, GranteeType.USER, originalOwnerUserId)
                .orElseGet(() -> {
                    ResourcePermission p = new ResourcePermission();
                    p.setResourceId(resourceId);
                    p.setGranteeType(GranteeType.USER);
                    p.setGranteeId(originalOwnerUserId);
                    return p;
                });
        permission.setPermissionLevel(permLevel);
        permission.setValidFrom(LocalDateTime.now());
        permission.setValidUntil(null);
        permission.setGrantedBy(operatorUserId);
        resourcePermissionRepository.save(permission);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 资源存在性校验 */
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

    /** 授权主体有效性校验：分组必须存在；用户必须存在 */
    private void validateGrantee(GranteeType granteeType, Long granteeId) {
        if (granteeType == GranteeType.GROUP) {
            if (!groupRepository.existsById(granteeId)) {
                throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
            }
        } else {
            if (!sysUserRepository.existsById(granteeId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "被授权用户不存在");
            }
        }
    }

    /** 校验目标用户曾为该资源的个人所有者，且当前不再是有效所有者（UC-034） */
    private void ensureOriginalOwner(Long resourceId, Long originalOwnerUserId) {
        boolean hadOwnership = resourceOwnerRepository
                .findByResourceIdAndOwnerTypeAndOwnerId(resourceId, OwnerType.USER, originalOwnerUserId)
                .isPresent();
        if (!hadOwnership) {
            throw new BusinessException(ErrorCode.ORIGINAL_OWNER_NOT_FOUND);
        }
        boolean currentlyEffectiveOwner = resourceOwnerRepository.findByResourceId(resourceId).stream()
                .filter(this::isEffective)
                .anyMatch(o -> o.getOwnerType() == OwnerType.USER && o.getOwnerId().equals(originalOwnerUserId));
        if (currentlyEffectiveOwner) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标用户当前仍为资源有效所有者，无法调整");
        }
    }

    /** 有效期合法性：起始可用日期必填，过期时间不能早于起始可用日期 */
    private void validateValidity(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validFrom == null) {
            throw new BusinessException(ErrorCode.OWNERSHIP_VALIDITY_INVALID, "起始可用日期不能为空");
        }
        if (validUntil != null && validUntil.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.OWNERSHIP_VALIDITY_INVALID);
        }
    }

    private ResourcePermissionResponse toResponse(ResourcePermission permission) {
        ResourcePermissionResponse response = new ResourcePermissionResponse();
        response.setPermissionId(permission.getPermissionId());
        response.setResourceId(permission.getResourceId());
        response.setGranteeType(permission.getGranteeType());
        response.setGranteeId(permission.getGranteeId());
        response.setPermissionLevel(permission.getPermissionLevel());
        response.setValidFrom(permission.getValidFrom());
        response.setValidUntil(permission.getValidUntil());
        response.setGrantedBy(permission.getGrantedBy());
        response.setCreatedAt(permission.getCreatedAt());
        return response;
    }
}

