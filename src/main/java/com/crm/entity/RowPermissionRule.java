package com.crm.entity;

import com.crm.common.enums.GranteeType;
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
 * 数据行级权限规则表 row_permission_rules。
 *
 * <p>基于业务对象属性（如地区、客户等级）过滤数据行，与资源权限叠加生效。
 * filter_expression 为 JSONB。
 */
@Getter
@Setter
@Entity
@Table(name = "row_permission_rules")
public class RowPermissionRule {

    /** 规则 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    /** 资源 ID */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** 授权主体类型：GROUP / USER */
    @Enumerated(EnumType.STRING)
    @Column(name = "grantee_type", nullable = false, length = 20)
    private GranteeType granteeType;

    /** 授权主体 ID */
    @Column(name = "grantee_id", nullable = false)
    private Long granteeId;

    /** 规则类型（如 REGION / CUSTOMER_LEVEL 等） */
    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    /** 过滤表达式（JSONB） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filter_expression", columnDefinition = "jsonb")
    private Map<String, Object> filterExpression;

    /** 状态：1-生效 0-停用 */
    @Column(nullable = false)
    private Integer status;

    /** 创建人用户 ID */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

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
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
