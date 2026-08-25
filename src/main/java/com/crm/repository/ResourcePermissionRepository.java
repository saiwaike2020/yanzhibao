package com.crm.repository;

import com.crm.common.enums.GranteeType;
import com.crm.entity.ResourcePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 资源显式授权表仓储。
 */
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, Long> {

    List<ResourcePermission> findByResourceId(Long resourceId);

    Optional<ResourcePermission> findByResourceIdAndPermissionId(Long resourceId, Long permissionId);

    Optional<ResourcePermission> findByResourceIdAndGranteeTypeAndGranteeId(Long resourceId, GranteeType granteeType, Long granteeId);

    boolean existsByResourceIdAndGranteeTypeAndGranteeId(Long resourceId, GranteeType granteeType, Long granteeId);

    /** 指定授权主体被授权的所有资源权限 */
    List<ResourcePermission> findByGranteeTypeAndGranteeId(GranteeType granteeType, Long granteeId);

    void deleteByResourceIdAndGranteeTypeAndGranteeId(Long resourceId, GranteeType granteeType, Long granteeId);
}
