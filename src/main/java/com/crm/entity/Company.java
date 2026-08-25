package com.crm.entity;

import com.crm.common.enums.CompanyStatus;
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
 * 企业表 companies（租户，承载企业空间、成员、分组和资源）。
 */
@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company {

    /** 企业 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    /** 企业唯一业务编号 */
    @Column(nullable = false, unique = true, length = 64)
    private String companyNo;

    /** 企业名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 企业 Logo URL */
    @Column(length = 255)
    private String logoUrl;

    /** 所有者用户 ID */
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /** 状态：1-正常 2-禁用 3-已解散 */
    @Column(nullable = false)
    private CompanyStatus status;

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
            status = CompanyStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
