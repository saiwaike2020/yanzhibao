package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 绑定 / 更换手机号请求（需短信验证码校验新手机号）。
 */
@Data
public class BindPhoneRequest {

    /** 新手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String newPhone;

    /** 发送到新手机号的短信验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String smsCode;
}
