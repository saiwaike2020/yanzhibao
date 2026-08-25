package com.crm.dto.auth;

import com.crm.common.enums.SystemRole;
import lombok.Data;

/**
 * 认证成功响应（JWT Token + 用户概要）。
 */
@Data
public class AuthTokenResponse {

    /** JWT Token */
    private String token;

    /** Token 类型，固定为 Bearer */
    private String tokenType = "Bearer";

    /** 有效期（秒） */
    private Long expiresIn;

    /** 用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 系统角色 */
    private SystemRole systemRole;
}
