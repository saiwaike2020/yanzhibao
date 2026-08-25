package com.crm.dto.audit;

import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
import lombok.Data;

/**
 * 审计 / 客服场景下查看的用户信息响应。
 */
@Data
public class AuditUserInfoResponse {

    /** 用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 昵称 */
    private String nickname;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 用户状态 */
    private UserStatus status;

    /** 系统角色 */
    private SystemRole systemRole;
}
