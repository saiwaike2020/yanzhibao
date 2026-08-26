package com.crm.common.exception;

import lombok.Getter;

/**
 * 统一错误码定义。
 * 1000 段为业务错误，可随业务扩展。
 */
@Getter
public enum ErrorCode {

    // ---- 通用 ----
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // ---- 认证 / 用户 ----
    PHONE_ALREADY_REGISTERED(1001, "该手机号已注册"),
    PHONE_NOT_REGISTERED(1002, "该手机号未注册"),
    WECHAT_ALREADY_BOUND(1003, "该微信已绑定其他账号"),
    AUTH_TYPE_MISSING(1004, "账号缺少可用的认证方式"),
    LOGIN_FAILED(1005, "手机号或密码错误"),
    ACCOUNT_DISABLED(1006, "账号已被禁用"),
    ACCOUNT_CANCELLED(1007, "账号已注销"),

    // ---- 短信验证码 ----
    SMS_SEND_TOO_FREQUENT(1101, "验证码发送过于频繁，请稍后再试"),
    SMS_CODE_INVALID(1102, "验证码错误或已失效"),
    SMS_CODE_EXPIRED(1103, "验证码已过期，请重新获取"),
    SMS_CODE_ATTEMPT_LIMIT(1104, "验证码错误次数过多，已失效"),
    SMS_MISSING_PHONE(1105, "操作者账号缺少安全验证手机号，请先完善手机绑定"),

    // ---- 企业 / 成员 ----
    COMPANY_NOT_FOUND(1201, "企业不存在"),
    COMPANY_DISSOLVED(1202, "企业已解散"),
    NOT_COMPANY_ADMIN(1203, "仅企业管理员可执行该操作"),
    NOT_COMPANY_OWNER(1204, "仅企业所有者可执行该操作"),
    MEMBER_NOT_FOUND(1205, "成员不存在或已退出"),
    OWNER_ROLE_NOT_CHANGEABLE(1206, "企业所有者角色不可直接更改，请先进行企业所有权转让"),
    INVITE_TARGET_EXISTS(1207, "目标用户已在该企业中"),
    APPROVAL_NOT_FOUND(1208, "审批申请不存在"),
    APPROVAL_ALREADY_HANDLED(1209, "该申请已处理"),
    NO_APPROVAL_PERMISSION(1210, "无审批权限"),
    NOT_COMPANY_MEMBER(1211, "您不是该企业成员"),

    // ---- 分组 / 授权 ----
    GROUP_NOT_FOUND(1301, "分组不存在"),
    SUB_GROUP_NOT_ALLOWED(1302, "当前仅支持创建一级分组"),
    GROUP_ADMIN_ONLY(1303, "仅企业管理员可创建分组"),
    DELEGATION_NOT_FOUND(1304, "管理授权记录不存在"),
    GRANTEE_NOT_COMPANY_MEMBER(1305, "被授权用户不是该企业成员"),

    // ---- 资源 / 权限 ----
    RESOURCE_NOT_FOUND(1401, "资源不存在"),
    PARENT_RESOURCE_INVALID(1402, "父级资源不存在或类型不合法"),
    PERMISSION_DENIED(1403, "没有足够的资源权限"),
    PERMISSION_NOT_FOUND(1404, "资源授权记录不存在"),
    ROW_PERMISSION_NOT_FOUND(1405, "数据行级权限规则不存在"),
    RESOURCE_OWNER_NOT_FOUND(1406, "资源所有者记录不存在"),
    NOT_RESOURCE_OWNER(1407, "操作者不是该资源的有效所有者"),
    OWNER_ALREADY_EXISTS(1408, "目标主体已是该资源的所有者"),
    OWNERSHIP_VALIDITY_INVALID(1409, "过期时间不能早于起始可用日期"),
    OWNERSHIP_ENDED(1410, "该所有权关系已过期或已撤销"),
    PERMISSION_ALREADY_EXISTS(1411, "该授权主体已有资源权限记录，请直接修改"),
    ORIGINAL_OWNER_NOT_FOUND(1412, "目标用户不是该资源的原所有者"),

    // ---- 消息中心 ----
    MESSAGE_NOT_FOUND(1501, "消息不存在"),

    // ---- 系统参数 / 存储配额 ----
    SETTING_NOT_FOUND(1601, "系统参数不存在"),
    STORAGE_QUOTA_EXCEEDED(1602, "存储空间不足，已达到配额上限"),
    INVALID_QUOTA_VALUE(1603, "存储配额必须为正整数"),
    STORAGE_QUOTA_NOT_FOUND(1604, "该主体未设置个体存储配额");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
