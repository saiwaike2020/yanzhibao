package com.crm.controller;

import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.audit.AuditUserInfoResponse;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.common.PageResponse;
import com.crm.security.SecurityUtils;
import com.crm.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计与客服相关接口（审计人员按授权范围查看日志、客服人员查看用户信息及日志）。
 *
 * <p>本组接口应配合 {@code @PreAuthorize("hasAnyRole('AUDITOR','SYSTEM_ADMIN','CUSTOMER_SERVICE')")} 使用。
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** 审计人员在其授权范围内查询审计日志 (UC-020) */
    @GetMapping("/logs")
    public ApiResponse<PageResponse<AuditLogResponse>> queryAuditLogs(@Valid AuditLogQueryRequest query) {
        return ApiResponse.ok(auditService.queryAuditLogs(SecurityUtils.getCurrentUserId(), query));
    }

    /** 客服人员验证服务对象后查看用户信息 (UC-021) */
    @GetMapping("/users/{userId}/info")
    public ApiResponse<AuditUserInfoResponse> getUserInfoForService(@PathVariable Long userId) {
        return ApiResponse.ok(auditService.getUserInfoForService(userId));
    }

    /** 客服人员查看指定用户权限范围内的日志 (UC-021) */
    @GetMapping("/users/{userId}/logs")
    public ApiResponse<PageResponse<AuditLogResponse>> queryUserLogs(
            @PathVariable Long userId, @Valid AuditLogQueryRequest query) {
        return ApiResponse.ok(auditService.queryUserLogs(userId, query));
    }
}
