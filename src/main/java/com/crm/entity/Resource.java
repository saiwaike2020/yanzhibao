package com.crm.entity;

import com.crm.common.enums.ResourceStatus;
import com.crm.common.enums.ResourceType;
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
 * 统一资源实体表 resources（资料库 LIBRARY / 文件夹 FOLDER / 文件 FILE）。
 *
 * <p>资源采用树状层级：资料库为顶层，其下可包含文件夹和文件；文件夹下可再包含子文件夹和文件。
 * 资源的所有权由 {@link ResourceOwner}（resource_owners）独立维护，支持多所有者、转让与有效期。
 */
@Getter
@Setter
@Entity
@Table(name = "resources")
public class Resource {

    /** 资源 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    /** 资源唯一业务编号 */
    @Column(name = "resource_no", nullable = false, unique = true, length = 64)
    private String resourceNo;

    /** 资源名称 */
    @Column(nullable = false, length = 255)
    private String name;

    /** 资源类型：LIBRARY / FOLDER / FILE */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    /** 父资源 ID（资料库为 null） */
    @Column(name = "parent_resource_id")
    private Long parentResourceId;

    /** 创建人用户 ID（仅记录创建者，不代表唯一所有者） */
    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文件 MIME 类型 */
    @Column(name = "file_type", length = 50)
    private String fileType;

    /** 文件存储路径 */
    @Column(name = "file_path", length = 512)
    private String filePath;

    /** 状态：1-正常 2-已归档 3-已删除 */
    @Column(nullable = false)
    private ResourceStatus status;

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
            status = ResourceStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
