package com.crm.common.enums;

/**
 * 短信验证码业务场景（sms_verifications.scene）。
 */
public enum SmsScene {
    /** 手机号验证码注册 */
    REGISTER,
    /** 登录 */
    LOGIN,
    /** 绑定/更换手机号 */
    BIND_PHONE,
    /** 变更企业管理员角色（提权/降级，UC-022） */
    ADMIN_ROLE_CHANGE,
    /** 设置分组管理员（分级授权，UC-008/UC-015） */
    GROUP_MANAGE_GRANT,
    /** 系统管理员分配审计人员权限（UC-019） */
    AUDITOR_ASSIGN,
    /** 重置密码 */
    RESET_PWD
}
