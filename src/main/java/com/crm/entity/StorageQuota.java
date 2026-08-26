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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 个体存储配额表 storage_quotas。
 *
 * <p>存储**单个用户 / 个别企业**的专属存储上限（字节），**优先于**全局默认配额
 * （system_settings），由系统管理员维护（UC-031）。
 */
@Getter
@Setter
@Entity
@Table(name = "storage_quotas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quota_subject", columnNames = {"quota_type", "subject_id"})
})
public class StorageQuota {

    /** 配额记录 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 主体类型：USER（用户）/ COMPANY（企业） */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 20)
    private OwnerType quotaType;

    /** 主体 ID（用户 ID 或企业 ID） */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** 专属存储上限（字节） */
    @Column(name = "quota_bytes", nullable = false)
    private Long quotaBytes;

    /** 最近更新人用户 ID（系统管理员） */
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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
