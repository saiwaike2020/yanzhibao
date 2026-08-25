package com.crm.repository;

import com.crm.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 系统审计日志表仓储。
 *
 * <p>通过 {@link JpaSpecificationExecutor} 支持按用户、企业、操作、时间范围等动态过滤。
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByCompanyId(Long companyId);
}
