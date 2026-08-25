package com.crm.dto.admin;

import lombok.Data;

/**
 * 系统参数响应。
 */
@Data
public class SystemSettingResponse {

    /** 参数键 */
    private String key;

    /** 参数值 */
    private String value;
}
