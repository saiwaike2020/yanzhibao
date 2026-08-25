package com.crm.entity;

import com.crm.common.enums.OwnerType;
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
import lombok.Getter;
import lombok.Setter;

/**
 * 资源所有者表 resource_owners。
 *
 * <p>维护资源的所有权关系：一个资源可被多个用户或企业共同拥有，所有权支持转让，
 * 并受有效期约束（起始可用日期 validFrom 必填，过期时间 validUntil 为空表示一直有效）。
 * 权限判定时仅统计 status=1 且当前时间处于 [validFrom, validUntil] 区间内的记录。
 */
@Getter
@Setter
@Entity
@Table(name = "resource_owners")
public class ResourceOwner {

    /** 所有权记录 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ownershipId;

    /** 资源 ID */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** 所有者类型：USER（个人用户）/ COMPANY（企业） */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;

    /** 所有者 ID（用户 ID 或企业 ID） */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 起始可用日期（必填） */
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    /** 过期时间（为空表示一直有效） */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** 授权/转让来源用户 ID（创建者或原所有者） */
    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    /** 状态：1-有效 0-已撤销/已转让 */
    @Column(nullable = false)
    private Integer status;

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