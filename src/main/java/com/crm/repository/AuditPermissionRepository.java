package com.crm.repository;

import com.crm.common.enums.AuditScope;
import com.crm.entity.AuditPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审计查看权限表仓储。
 */
public interface AuditPermissionRepository extends JpaRepository<AuditPermission, Long> {

    Optional<AuditPermission> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<AuditPermission> findByAuditScope(AuditScope auditScope);
}
