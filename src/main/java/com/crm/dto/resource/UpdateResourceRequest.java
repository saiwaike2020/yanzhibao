package com.crm.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新资源请求（重命名、移动等）。
 */
@Data
public class UpdateResourceRequest {

    /** 资源名称 */
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 255, message = "资源名称不能超过 255 个字符")
    private String name;

    /** 移动后的父资源 ID（不移动时传原值） */
    private Long parentResourceId;
}
