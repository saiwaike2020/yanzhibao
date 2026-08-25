package com.crm.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统参数配置请求。
 */
@Data
public class SystemSettingRequest {

    /** 参数键 */
    @NotBlank(message = "参数键不能为空")
    private String key;

    /** 参数值 */
    @NotBlank(message = "参数值不能为空")
    private String value;
}
