package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码 / 手机号验证码重置密码请求 (UC-025 / 设计文档 4.7)。
 */
@Data
public class ResetPasswordRequest {

    /** 已注册手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码（场景 RESET_PWD） */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String smsCode;

    /** 新密码：至少 8 位，需包含字母和数字 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String newPassword;
}
