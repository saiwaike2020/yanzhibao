package com.crm.dto.auth;

import lombok.Data;

/**
 * 微信扫码登录二维码响应。
 */
@Data
public class WechatQrcodeResponse {

    /** 登录二维码图片 URL */
    private String qrcodeUrl;

    /** 一次性 state（扫码回调时原样带回校验） */
    private String state;

    /** 二维码过期时间（秒） */
    private Integer expiresIn;
}
