package com.crm.dto.resource;

import com.crm.common.enums.ResourceStatus;
import com.crm.common.enums.ResourceType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资源信息响应（resources）。
 * 注：资源的所有权不在此返回，通过 /api/resources/{resourceId}/owners 查询。
 */
@Data
public class ResourceResponse {

    /** 资源 ID */
    private Long resourceId;

    /** 资源编号 */
    private String resourceNo;

    /** 资源名称 */
    private String name;

    /** 资源类型 */
    private ResourceType resourceType;

    /** 父资源 ID */
    private Long parentResourceId;

    /** 创建人用户 ID（仅记录创建者，不代表唯一所有者） */
    private Long creatorUserId;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 MIME 类型 */
    private String fileType;

    /** 文件存储路径 */
    private String filePath;

    /** 资源状态 */
    private ResourceStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
