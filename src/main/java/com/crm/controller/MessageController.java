package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.message.MarkMessagesReadRequest;
import com.crm.dto.message.UserMessageResponse;
import com.crm.security.SecurityUtils;
import com.crm.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息中心接口（查询我的消息、未读数、标记已读 / 全部已读）。需登录。
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 分页查询我的消息 */
    @GetMapping
    public ApiResponse<PageResponse<UserMessageResponse>> listMessages(@Valid PageQueryRequest query) {
        return ApiResponse.ok(messageService.listMessages(SecurityUtils.getCurrentUserId(), query));
    }

    /** 未读消息数 */
    @GetMapping("/unread-count")
    public ApiResponse<Long> countUnread() {
        return ApiResponse.ok(messageService.countUnread(SecurityUtils.getCurrentUserId()));
    }

    /** 标记单条消息已读 */
    @PutMapping("/{messageId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long messageId) {
        messageService.markRead(SecurityUtils.getCurrentUserId(), messageId);
        return ApiResponse.ok();
    }

    /** 批量标记已读 */
    @PutMapping("/read-batch")
    public ApiResponse<Void> markReadBatch(@Valid @RequestBody MarkMessagesReadRequest request) {
        messageService.markRead(SecurityUtils.getCurrentUserId(), request.getMessageIds());
        return ApiResponse.ok();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public ApiResponse<Integer> markAllRead() {
        return ApiResponse.ok(messageService.markAllRead(SecurityUtils.getCurrentUserId()));
    }
}
