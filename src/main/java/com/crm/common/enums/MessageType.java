package com.crm.common.enums;

/**
 * 消息中心消息类型（user_messages.message_type）。
 */
public enum MessageType {
    /** 系统消息（系统 / 管理员主动发送） */
    SYSTEM,
    /** 申请加入企业（用户 → 企业管理员，UC-026） */
    JOIN_REQUEST,
    /** 企业邀请加入（企业管理员 → 被邀请用户，UC-027） */
    INVITATION
}
