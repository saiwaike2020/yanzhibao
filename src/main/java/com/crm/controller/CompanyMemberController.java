package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.company.ChangeMemberRoleRequest;
import com.crm.dto.company.CompanyMemberResponse;
import com.crm.dto.company.InviteMemberRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.CompanyMemberService;
import jakarta.validation.Valid;
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
 * 企业成员管理接口（邀请成员、成员角色变更、禁用 / 恢复、移除、退出企业）。
 */
@RestController
@RequestMapping("/api/companies/{companyId}/members")
@RequiredArgsConstructor
public class CompanyMemberController {

    private final CompanyMemberService companyMemberService;

    /** 邀请成员加入企业 (UC-006 / UC-027)：成功后向被邀请用户发送邀请消息 */
    @PostMapping("/invite")
    public ApiResponse<Void> inviteMember(
            @PathVariable Long companyId, @Valid @RequestBody InviteMemberRequest request) {
        companyMemberService.inviteMember(companyId, SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 用户申请加入企业 (UC-026)：成功后向企业管理员发送申请消息 */
    @PostMapping("/apply")
    public ApiResponse<Void> applyJoinCompany(@PathVariable Long companyId) {
        companyMemberService.applyJoinCompany(companyId, SecurityUtils.getCurrentUserId());
        return ApiResponse.ok();
    }

    /** 成员列表 */
    @GetMapping
    public ApiResponse<PageResponse<CompanyMemberResponse>> listMembers(
            @PathVariable Long companyId, @Valid PageQueryRequest query) {
        return ApiResponse.ok(companyMemberService.listMembers(companyId, query));
    }

    /** 成员详情 */
    @GetMapping("/{memberId}")
    public ApiResponse<CompanyMemberResponse> getMember(
            @PathVariable Long companyId, @PathVariable Long memberId) {
        return ApiResponse.ok(companyMemberService.getMember(companyId, memberId));
    }

    /** 设置 / 取消企业管理员（短信验证，UC-022） */
    @PutMapping("/{memberId}/role")
    public ApiResponse<Void> changeMemberRole(
            @PathVariable Long companyId,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeMemberRoleRequest request) {
        companyMemberService.changeMemberRole(companyId, memberId, request);
        return ApiResponse.ok();
    }

    /** 禁用成员 */
    @PutMapping("/{memberId}/disable")
    public ApiResponse<Void> disableMember(@PathVariable Long companyId, @PathVariable Long memberId) {
        companyMemberService.disableMember(companyId, memberId);
        return ApiResponse.ok();
    }

    /** 恢复成员 */
    @PutMapping("/{memberId}/restore")
    public ApiResponse<Void> restoreMember(@PathVariable Long companyId, @PathVariable Long memberId) {
        companyMemberService.restoreMember(companyId, memberId);
        return ApiResponse.ok();
    }

    /** 移除成员 / 撤销权限 (UC-016) */
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable Long companyId, @PathVariable Long memberId) {
        companyMemberService.removeMember(companyId, memberId);
        return ApiResponse.ok();
    }

    /** 退出企业 (UC-017) */
    @PostMapping("/leave")
    public ApiResponse<Void> leaveCompany(@PathVariable Long companyId) {
        companyMemberService.leaveCompany(companyId, SecurityUtils.getCurrentUserId());
        return ApiResponse.ok();
    }
}
