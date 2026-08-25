package com.crm.common.enums;

/**
 * 审计人员查看范围（audit_permissions.audit_scope）。
 */
public enum AuditScope {
    /** 查看全部日志（不含系统用户敏感操作日志，可由系统管理员单独控制） */
    ALL,
    /** 仅查看普通用户日志 */
    REGULAR_USERS,
    /** 仅查看企业用户日志 */
    ENTERPRISE_USERS
}
