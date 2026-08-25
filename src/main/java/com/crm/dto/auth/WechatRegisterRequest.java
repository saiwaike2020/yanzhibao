package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信快速注册请求 (UC-002)：微信授权 + 手机号 + 短信验证码。
 */
@Data
public class WechatRegisterRequest {

    /** 微信 OAuth 授权回调 code */
    @NotBlank(message = "微信授权 code 不能为空")
    private String wechatCode;

    /** 一次性 state（防 CSRF） */
    @NotBlank(message = "state 不能为空")
    private String state;

    /** 绑定手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String smsCode;

    /** 登录密码（可选，跳过则后续仅可通过微信扫码登录） */
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String password;
}
