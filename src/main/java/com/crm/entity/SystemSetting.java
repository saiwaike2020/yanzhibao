package com.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 系统参数表 system_settings（K/V 结构）。
 *
 * <p>存储系统级全局配置，如个人 / 企业存储配额上限
 * （storage.quota.personal / storage.quota.company），由系统管理员维护（UC-030）。
 */
@Getter
@Setter
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    /** 参数 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 参数键（唯一） */
    @Column(name = "setting_key", nullable = false, unique = true, length = 64)
    private String settingKey;

    /** 参数值 */
    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    /** 参数说明 */
    @Column(length = 255)
    private String description;

    /** 最近更新人用户 ID */
    @Column(name = "updated_by")
    private Long updatedBy;

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
        if (settingValue == null) {
            settingValue = "";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
