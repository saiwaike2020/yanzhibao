package com.crm.entity;

import com.crm.common.enums.GranteeType;
import com.crm.common.enums.PermissionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 资源显式授权表 resource_permissions。
 *
 * <p>授权主体支持分组（GROUP）和用户（USER）；权限级别 READ / WRITE / OWNER。
 * 唯一约束 (resource_id, grantee_type, grantee_id)；撤销权限为删除记录。
 * <p>权限有效期：起始可用日期 {@code validFrom} 必填，过期时间 {@code validUntil} 为空表示一直有效；
 * 权限判定仅统计当前时间处于 [validFrom, validUntil] 区间内的记录（设计文档 6.4 / 6.6）。
 */
@Getter
@Setter
@Entity
@Table(name = "resource_permissions", indexes = {
        @Index(name = "idx_grantee_validity", columnList = "resource_id, permission_level, valid_from, valid_until")
})
public class ResourcePermission {

    /** 权限记录 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    /** 资源 ID */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** 授权主体类型：GROUP / USER */
    @Enumerated(EnumType.STRING)
    @Column(name = "grantee_type", nullable = false, length = 20)
    private GranteeType granteeType;

    /** 授权主体 ID（分组 ID 或用户 ID） */
    @Column(name = "grantee_id", nullable = false)
    private Long granteeId;

    /** 权限级别：READ / WRITE / OWNER */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel;

    /** 起始可用日期（必填） */
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    /** 过期时间（为空表示一直有效） */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** 授权人用户 ID */
    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

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
