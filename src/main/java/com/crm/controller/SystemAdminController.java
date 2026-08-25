package com.crm.controller;

import com.crm.dto.admin.AssignAuditorRequest;
import com.crm.dto.admin.AuditorResponse;
import com.crm.dto.admin.SystemSettingRequest;
import com.crm.dto.admin.SystemSettingResponse;
import com.crm.dto.admin.UpdateAuditorRequest;
import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.user.UserProfileResponse;
import com.crm.service.SystemAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理员管理接口（系统级最高权限：用户管理、审计人员权限分配、系统参数、全部审计日志）。
 *
 * <p>本组接口应配合 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 使用。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    /** 用户列表（分页 / 关键字查询） */
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserProfileResponse>> listUsers(@Valid PageQueryRequest query) {
        return ApiResponse.ok(systemAdminService.listUsers(query));
    }

    /** 用户详情 */
    @GetMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.ok(systemAdminService.getUserDetail(userId));
    }

    /** 禁用用户 */
    @PutMapping("/users/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long userId) {
        systemAdminService.disableUser(userId);
        return ApiResponse.ok();
    }

    /** 恢复用户 */
    @PutMapping("/users/{userId}/restore")
    public ApiResponse<Void> restoreUser(@PathVariable Long userId) {
        systemAdminService.restoreUser(userId);
        return ApiResponse.ok();
    }

    /** 注销用户 */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> cancelUser(@PathVariable Long userId) {
        systemAdminService.cancelUser(userId);
        return ApiResponse.ok();
    }

    /** 审计人员列表 */
    @GetMapping("/auditors")
    public ApiResponse<List<AuditorResponse>> listAuditors() {
        return ApiResponse.ok(systemAdminService.listAuditors());
    }

    /** 分配审计人员权限及查看范围（短信验证，UC-019） */
    @PostMapping("/auditors")
    public ApiResponse<Void> assignAuditor(@Valid @RequestBody AssignAuditorRequest request) {
        systemAdminService.assignAuditor(request);
        return ApiResponse.ok();
    }

    /** 调整审计人员权限 / 查看范围（短信验证） */
    @PutMapping("/auditors/{userId}")
    public ApiResponse<Void> updateAuditor(@PathVariable Long userId, @Valid @RequestBody UpdateAuditorRequest request) {
        systemAdminService.updateAuditor(userId, request);
        return ApiResponse.ok();
    }

    /** 撤销审计角色 */
    @DeleteMapping("/auditors/{userId}")
    public ApiResponse<Void> revokeAuditor(@PathVariable Long userId) {
        systemAdminService.revokeAuditor(userId);
        return ApiResponse.ok();
    }

    /** 查看全部审计日志 */
    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> listAllAuditLogs(@Valid AuditLogQueryRequest query) {
        return ApiResponse.ok(systemAdminService.listAllAuditLogs(query));
    }

    /** 查询系统参数 */
    @GetMapping("/settings/{key}")
    public ApiResponse<SystemSettingResponse> getSystemSetting(@PathVariable String key) {
        return ApiResponse.ok(systemAdminService.getSystemSetting(key));
    }

    /** 配置系统参数 */
    @PutMapping("/settings")
    public ApiResponse<Void> updateSystemSetting(@Valid @RequestBody SystemSettingRequest request) {
        systemAdminService.updateSystemSetting(request);
        return ApiResponse.ok();
    }
}
