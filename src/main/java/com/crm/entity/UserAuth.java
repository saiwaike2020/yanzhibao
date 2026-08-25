package com.crm.entity;

import com.crm.common.enums.AuthType;
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
 * 认证标识表 user_auths。
 *
 * <p>一个用户可绑定手机号认证与一个微信认证；
 * 唯一约束 (auth_type, identifier) 保证一个手机号或微信只能绑定一个用户。
 */
@Getter
@Setter
@Entity
@Table(name = "user_auths")
public class UserAuth {

    /** 认证 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authId;

    /** 关联用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 认证类型：PHONE / WECHAT */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthType authType;

    /** 认证标识：手机号或微信 unionid/openid */
    @Column(nullable = false, length = 128)
    private String identifier;

    /** 凭证（密码哈希；微信认证可为空） */
    @Column(nullable = false, length = 255)
    private String credential;

    /** 认证/验证通过时间 */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** 状态：1-启用 0-禁用 */
    @Column(nullable = false)
    private Integer status;

    /** 创建/绑定时间 */
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
