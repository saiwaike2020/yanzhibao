package com.crm.entity;

import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
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
 * 用户主表 sys_users。
 *
 * <p>不区分普通用户/企业用户，企业身份通过 {@link CompanyMember} 表达；
 * 系统角色通过 system_role 标记（NONE / SYSTEM_ADMIN / AUDITOR / CUSTOMER_SERVICE）。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_users")
public class SysUser {

    /** 用户 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    /** 用户全局唯一业务编号 */
    @Column(nullable = false, unique = true, length = 64)
    private String userNo;

    /** 用户主手机号（敏感操作短信校验使用） */
    @Column(length = 20)
    private String phone;

    /** 昵称 */
    @Column(nullable = false, length = 64)
    private String nickname;

    /** 头像 URL */
    @Column(length = 255)
    private String avatarUrl;

    /** 状态：1-正常 2-禁用 3-已注销 */
    @Column(nullable = false)
    private UserStatus status;

    /** 系统级角色 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SystemRole systemRole;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 软删除标记 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
            status = UserStatus.ACTIVE;
        }
        if (systemRole == null) {
            systemRole = SystemRole.NONE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
