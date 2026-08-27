package com.crm.entity;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.MemberStatus;
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
 * 企业成员关联表 company_members。
 *
 * <p>一个用户可同时加入多个企业，在不同企业中角色可以不同。
 * 唯一约束 (company_id, user_id)。
 */
@Getter
@Setter
@Entity
@Table(name = "company_members")
public class CompanyMember {

    /** 成员关系 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    /** 企业 ID */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** 用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 企业内角色：OWNER / ADMIN / MEMBER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyMemberRole role;

    /** 成员状态：0-已邀请 1-正常 2-禁用 3-已退出 */
    @Column(nullable = false)
    private MemberStatus status;

    /** 加入时间 */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    /** 记录生效起始时间（v3.7） */
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    /** 记录失效时间（v3.7，NULL 表示一直有效；退出/移除设此字段） */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

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
        if (validFrom == null) {
            validFrom = now;
        }
        if (status == null) {
            status = MemberStatus.ACTIVE;
        }
        if (role == null) {
            role = CompanyMemberRole.MEMBER;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
