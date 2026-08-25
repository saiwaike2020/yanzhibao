package com.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 系统审计日志表 audit_logs。
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    /** 日志 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    /** 操作用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 操作用户类型：USER / COMPANY_USER / SYSTEM */
    @Column(name = "user_type", nullable = false, length = 32)
    private String userType;

    /** 所属企业 ID */
    @Column(name = "company_id")
    private Long companyId;

    /** 操作行为 */
    @Column(nullable = false, length = 64)
    private String action;

    /** 资源类型 */
    @Column(name = "resource_type", length = 32)
    private String resourceType;

    /** 资源 ID */
    @Column(name = "resource_id", length = 64)
    private String resourceId;

    /** 操作详情（JSONB） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> detail;

    /** 客户端 IP */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** 状态：1-正常 */
    @Column(nullable = false)
    private Integer status;

    /** 记录时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }
}
