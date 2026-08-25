package com.crm.service;

import com.crm.common.enums.MessageType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.message.UserMessageResponse;
import com.crm.entity.UserMessage;
import com.crm.repository.UserMessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息中心服务（发送系统消息 / 申请 / 邀请消息；分页查询、未读数、标记已读）。
 *
 * <p>当前版本**不通过短信发送额外提醒**，仅站内消息；{@code sms_notified} 字段与
 * {@link #sendSmsReminder(UserMessage)} 预留短信通知扩展点，后续接入短信网关时实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final UserMessageRepository userMessageRepository;

    /** 发送系统消息（SYSTEM，由系统 / 管理员主动发送） */
    @Transactional
    public void sendSystemMessage(Long userId, String title, String content) {
        sendMessage(userId, MessageType.SYSTEM, title, content, null, null);
    }

    /** 发送「申请加入企业」消息（JOIN_REQUEST，发给企业管理员，UC-026） */
    @Transactional
    public void sendJoinRequestMessage(Long adminUserId, Long applicantUserId, Long companyId, String companyName) {
        String title = "新的企业加入申请";
        String content = "用户申请加入企业【" + companyName + "】，请前往成员管理页面处理。";
        sendMessage(adminUserId, MessageType.JOIN_REQUEST, title, content, companyId, applicantUserId);
    }

    /** 发送「企业邀请加入」消息（INVITATION，发给被邀请用户，UC-027） */
    @Transactional
    public void sendInvitationMessage(Long targetUserId, Long inviterUserId, Long companyId, String companyName) {
        String title = "企业加入邀请";
        String content = "企业【" + companyName + "】邀请您加入，请前往消息中心确认。";
        sendMessage(targetUserId, MessageType.INVITATION, title, content, companyId, inviterUserId);
    }

    /** 分页查询我的消息（按创建时间倒序） */
    public PageResponse<UserMessageResponse> listMessages(Long userId, PageQueryRequest query) {
        PageRequest pageable = PageRequest.of(query.getPage() - 1, query.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserMessage> page = userMessageRepository.findByUserId(userId, pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                query.getPage(),
                query.getSize());
    }

    /** 未读消息数 */
    public long countUnread(Long userId) {
        return userMessageRepository.countByUserIdAndIsRead(userId, 0);
    }

    /** 标记单条消息已读（校验归属） */
    @Transactional
    public void markRead(Long userId, Long messageId) {
        UserMessage message = userMessageRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (message.getIsRead() == null || message.getIsRead() != 1) {
            message.setIsRead(1);
            message.setReadAt(LocalDateTime.now());
            userMessageRepository.save(message);
        }
    }

    /** 批量标记已读 */
    @Transactional
    public void markRead(Long userId, List<Long> messageIds) {
        messageIds.forEach(id -> markRead(userId, id));
    }

    /** 全部标记已读，返回受影响条数 */
    @Transactional
    public int markAllRead(Long userId) {
        return userMessageRepository.markAllRead(userId);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    private void sendMessage(Long userId, MessageType type, String title, String content,
                             Long relatedCompanyId, Long relatedUserId) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setMessageType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedCompanyId(relatedCompanyId);
        message.setRelatedUserId(relatedUserId);
        message.setIsRead(0);
        message.setSmsNotified(0);
        userMessageRepository.save(message);

        log.info("站内消息已发送: userId={}, type={}, title={}", userId, type, title);

        // 预留短信提醒扩展点：当前版本不通过短信发送额外提醒
        sendSmsReminder(message);
    }

    /** 预留：短信提醒发送（当前空实现，不真正发送短信；后续接入短信网关时实现并置 sms_notified=1） */
    private void sendSmsReminder(UserMessage message) {
        log.info("[消息中心] 短信提醒预留，暂不发送: userId={}, type={}, messageId={}",
                message.getUserId(), message.getMessageType(), message.getMessageId());
    }

    private UserMessageResponse toResponse(UserMessage message) {
        UserMessageResponse response = new UserMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setMessageType(message.getMessageType());
        response.setTitle(message.getTitle());
        response.setContent(message.getContent());
        response.setRelatedCompanyId(message.getRelatedCompanyId());
        response.setRelatedUserId(message.getRelatedUserId());
        response.setIsRead(message.getIsRead());
        response.setReadAt(message.getReadAt());
        response.setSmsNotified(message.getSmsNotified());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
