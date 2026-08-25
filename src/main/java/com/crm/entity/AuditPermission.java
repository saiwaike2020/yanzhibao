package com.crm.entity;

import com.crm.common.enums.AuditScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 审计查看权限表 audit_permissions。
 *
 * <p>系统管理员为审计人员配置日志查看范围（audit_scope），唯一约束 user_id。
 * scope_details 为 JSONB（如指定用户/企业范围）。
 */
@Getter
@Setter
@Entity
@Table(name = "audit_permissions")
public class AuditPermission {

    /** 主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 审计人员用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 审计日志查看范围：ALL / REGULAR_USERS / ENTERPRISE_USERS */
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_scope", nullable = false, length = 32)
    private AuditScope auditScope;

    /** 查看范围明细（JSONB） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_details", columnDefinition = "jsonb")
    private Map<String, Object> scopeDetails;

    /** 授权人用户 ID */
    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    /** 过期时间 */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
