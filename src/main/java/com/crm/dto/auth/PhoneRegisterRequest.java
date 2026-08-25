package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号 + 短信验证码注册请求 (UC-001)。
 */
@Data
public class PhoneRegisterRequest {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String smsCode;

    /** 登录密码：至少 8 位，需包含字母和数字 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String password;

    /** 昵称（可选） */
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    /** 头像 URL（可选） */
    @Size(max = 255, message = "头像 URL 过长")
    private String avatarUrl;
}
