package com.crm.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建分组请求 (UC-007)。
 * 当前仅支持一级分组：parentGroupId 必须为空。
 */
@Data
public class CreateGroupRequest {

    /** 分组名称 */
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "分组名称不能超过 64 个字符")
    private String name;

    /** 分组描述 */
    @Size(max = 255, message = "分组描述不能超过 255 个字符")
    private String description;

    /**
     * 父分组 ID。
     * 当前阶段仅允许创建一级分组，此字段必须为 null；
     * 后续扩展树状层级时开放。
     */
    private Long parentGroupId;
}
