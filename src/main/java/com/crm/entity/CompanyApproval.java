package com.crm.entity;

import com.crm.common.enums.ApprovalStatus;
import com.crm.common.enums.ApprovalType;
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
 * 企业变更审批表 company_approvals。
 *
 * <p>记录企业**注销**与**所有权转让**的审批流程（v3.5）：由企业所有者发起，
 * 须经**系统管理员或有权限的审计人员**批准后方可生效（UC-033）。
 */
@Getter
@Setter
@Entity
@Table(name = "company_approvals", indexes = {
        @Index(name = "idx_approval_company", columnList = "company_id, status"),
        @Index(name = "idx_approval_status", columnList = "status, created_at")
})
public class CompanyApproval {

    /** 审批记录 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    /** 关联企业 ID */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** 审批类型：DISSOLVE（注销）/ TRANSFER（所有权转让） */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 20)
    private ApprovalType approvalType;

    /** 发起人用户 ID（企业所有者） */
    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    /** 转让目标用户 ID（仅 TRANSFER 类型） */
    @Column(name = "target_user_id")
    private Long targetUserId;

    /** 状态：PENDING / APPROVED / REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    /** 审批人用户 ID（系统管理员或有权限的审计人员） */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /** 审批意见 */
    @Column(name = "review_note", length = 255)
    private String reviewNote;

    /** 审批时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 申请创建时间 */
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
            status = ApprovalStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
