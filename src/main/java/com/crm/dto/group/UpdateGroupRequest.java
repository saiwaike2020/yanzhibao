package com.crm.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新分组请求。
 */
@Data
public class UpdateGroupRequest {

    /** 分组名称 */
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "分组名称不能超过 64 个字符")
    private String name;

    /** 分组描述 */
    @Size(max = 255, message = "分组描述不能超过 255 个字符")
    private String description;
}
