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
 * 企业分组表 groups。
 *
 * <p>支持树状层级（parentGroupId），但当前阶段仅允许一级分组（parentGroupId 为 null）。
 */
@Getter
@Setter
@Entity
@Table(name = "groups")
public class Group {

    /** 分组 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    /** 所属企业 ID */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** 父分组 ID（当前一级分组为 null） */
    @Column(name = "parent_group_id")
    private Long parentGroupId;

    /** 分组名称 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 分组描述 */
    @Column(length = 255)
    private String description;

    /** 状态：1-正常 0-禁用 */
    @Column(nullable = false)
    private Integer status;

    /** 创建人用户 ID */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

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
        if (description == null) {
            description = "";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
