package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手机号 + 密码登录请求 (UC-003)。
 */
@Data
public class PhonePasswordLoginRequest {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
