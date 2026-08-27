package com.crm.dto.resource;

import com.crm.common.enums.OwnerType;
import com.crm.common.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建资源请求 (UC-010)。
 * 支持资料库（LIBRARY，parentResourceId 为空）、文件夹、文件。
 *
 * <p>ownerType/ownerId 表示资源创建后默认登记为所有者的主体：
 * 个人资源 ownerType=USER，企业资源 ownerType=COMPANY；
 * 系统创建资源的同时写入 resource_owners（起始可用日期=创建时间，永久有效）。
 */
@Data
public class CreateResourceRequest {

    /** 资源类型：LIBRARY / FOLDER / FILE */
    @NotNull(message = "资源类型不能为空")
    private ResourceType resourceType;

    /** 资源名称 */
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 255, message = "资源名称不能超过 255 个字符")
    private String name;

    /** 父资源 ID（资料库为 null，文件夹/文件必填） */
    private Long parentResourceId;

    /** 所有者类型：USER（个人空间）/ COMPANY（企业空间），创建后登记为初始所有者 */
    @NotNull(message = "所有者类型不能为空")
    private OwnerType ownerType;

    /** 所有者 ID（用户 ID 或企业 ID），创建后登记为初始所有者 */
    @NotNull(message = "所有者 ID 不能为空")
    private Long ownerId;

    /** 文件大小（字节，仅 FILE 类型） */
    private Long fileSize;

    /** 文件 MIME 类型（仅 FILE 类型） */
    @Size(max = 50, message = "文件类型不能超过 50 个字符")
    private String fileType;

    /** 文件相对抽象存储标识符（仅 FILE 类型，由存储服务生成，不存绝对路径） */
    @Size(max = 512, message = "文件标识过长")
    private String fileKey;
}
