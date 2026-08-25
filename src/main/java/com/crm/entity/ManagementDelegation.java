package com.crm.entity;

import com.crm.common.enums.DelegationScope;
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
 * 分级管理授权表 management_delegations。
 *
 * <p>企业管理员将指定分组的管理权授予成员，使其成为分组管理员。
 */
@Getter
@Setter
@Entity
@Table(name = "management_delegations")
public class ManagementDelegation {

    /** 授权 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long delegationId;

    /** 企业 ID */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** 被管理的分组 ID */
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** 被授权用户 ID（分组管理员） */
    @Column(name = "grantee_user_id", nullable = false)
    private Long granteeUserId;

    /** 授权人用户 ID */
    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    /** 授权范围：GROUP_MANAGE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DelegationScope scope;

    /** 状态：1-有效 0-已撤销 */
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
        if (scope == null) {
            scope = DelegationScope.GROUP_MANAGE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
