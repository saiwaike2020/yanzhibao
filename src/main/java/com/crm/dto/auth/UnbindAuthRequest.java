package com.crm.dto.auth;

import com.crm.common.enums.AuthType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 解绑认证方式请求（至少保留一种登录方式）。
 */
@Data
public class UnbindAuthRequest {

    /** 要解绑的认证类型：PHONE / WECHAT */
    @NotNull(message = "认证类型不能为空")
    private AuthType authType;

    /** 认证标识（可选；为空时默认解绑当前用户该类型的唯一绑定） */
    private String identifier;
}
