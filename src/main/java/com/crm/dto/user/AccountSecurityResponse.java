package com.crm.dto.user;

import com.crm.common.enums.AuthType;
import java.util.List;
import lombok.Data;

/**
 * 账号安全信息响应（已绑定认证方式、是否设置密码等）。
 */
@Data
public class AccountSecurityResponse {

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 已绑定的认证方式 */
    private List<AuthType> boundAuthTypes;

    /** 是否已设置登录密码（false 则只能通过微信扫码登录） */
    private boolean hasPassword;
}
