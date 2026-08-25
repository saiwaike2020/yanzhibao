package com.crm.entity;

import com.crm.common.enums.SmsScene;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 短信验证码记录表 sms_verifications。
 */
@Getter
@Setter
@Entity
@Table(name = "sms_verifications")
public class SmsVerification {

    /** 主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收验证码手机号 */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 验证码哈希 */
    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    /** 业务场景：REGISTER / LOGIN / BIND_PHONE / ADMIN_ROLE_CHANGE 等 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SmsScene scene;

    /** 过期时间 */
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    /** 使用消耗时间 */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** 尝试校验错误次数 */
    @Column(nullable = false)
    private Integer attempts;

    /** 请求客户端 IP */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 发送时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attempts == null) {
            attempts = 0;
        }
    }
}
