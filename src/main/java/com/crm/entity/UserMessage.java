package com.crm.entity;

import com.crm.common.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户消息中心表 user_messages。
 *
 * <p>每个用户拥有独立消息列表，接收系统消息。消息类型：SYSTEM / JOIN_REQUEST / INVITATION。
 * {@code smsNotified} 预留短信通知扩展点（当前版本不通过短信发送额外提醒）。
 */
@Getter
@Setter
@Entity
@Table(name = "user_messages", indexes = {
        @Index(name = "idx_msg_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_msg_user_read", columnList = "user_id, is_read")
})
public class UserMessage {

    /** 消息 ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    /** 接收者用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 消息类型：SYSTEM / JOIN_REQUEST / INVITATION */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    /** 消息标题 */
    @Column(nullable = false, length = 128)
    private String title;

    /** 消息正文 */
    @Column(nullable = false, length = 512)
    private String content;

    /** 关联企业 ID（申请 / 邀请消息填充） */
    @Column(name = "related_company_id")
    private Long relatedCompanyId;

    /** 关联用户 ID（申请发起人 / 邀请发起人） */
    @Column(name = "related_user_id")
    private Long relatedUserId;

    /** 是否已读：0-未读 1-已读 */
    @Column(name = "is_read", nullable = false)
    private Integer isRead;

    /** 已读时间 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** 短信通知标记：0-未发短信（当前固定），1-已发短信（后续短信通道接入后） */
    @Column(name = "sms_notified", nullable = false)
    private Integer smsNotified;

    /** 消息创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (isRead == null) {
            isRead = 0;
        }
        if (smsNotified == null) {
            smsNotified = 0;
        }
    }
}
