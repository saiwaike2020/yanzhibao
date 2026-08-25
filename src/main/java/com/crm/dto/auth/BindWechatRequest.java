package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定微信请求。
 */
@Data
public class BindWechatRequest {

    /** 微信 OAuth 授权回调 code */
    @NotBlank(message = "微信授权 code 不能为空")
    private String code;

    /** 一次性 state（防 CSRF） */
    @NotBlank(message = "state 不能为空")
    private String state;
}
