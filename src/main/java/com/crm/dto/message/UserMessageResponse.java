package com.crm.dto.message;

import com.crm.common.enums.MessageType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户消息响应（消息中心）。
 */
@Data
public class UserMessageResponse {

    /** 消息 ID */
    private Long messageId;

    /** 消息类型 */
    private MessageType messageType;

    /** 消息标题 */
    private String title;

    /** 消息正文 */
    private String content;

    /** 关联企业 ID */
    private Long relatedCompanyId;

    /** 关联用户 ID（申请发起人 / 邀请发起人） */
    private Long relatedUserId;

    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;

    /** 已读时间 */
    private LocalDateTime readAt;

    /** 短信通知标记（当前固定 0，预留短信扩展点） */
    private Integer smsNotified;

    /** 消息创建时间 */
    private LocalDateTime createdAt;
}
