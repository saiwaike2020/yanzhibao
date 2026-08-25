package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.delegation.CreateDelegationRequest;
import com.crm.dto.delegation.DelegationResponse;
import com.crm.service.ManagementDelegationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分级授权管理接口（企业管理员将指定分组的管理权授予其他成员，使其成为分组管理员）。
 */
@RestController
@RequestMapping("/api/companies/{companyId}/delegations")
@RequiredArgsConstructor
public class ManagementDelegationController {

    private final ManagementDelegationService managementDelegationService;

    /** 授予成员指定分组的管理权（设置分组管理员，短信验证，UC-008 / UC-015） */
    @PostMapping
    public ApiResponse<DelegationResponse> createDelegation(
            @PathVariable Long companyId, @Valid @RequestBody CreateDelegationRequest request) {
        return ApiResponse.ok(managementDelegationService.createDelegation(companyId, request));
    }

    /** 管理授权列表 */
    @GetMapping
    public ApiResponse<List<DelegationResponse>> listDelegations(@PathVariable Long companyId) {
        return ApiResponse.ok(managementDelegationService.listDelegations(companyId));
    }

    /** 授权详情 */
    @GetMapping("/{delegationId}")
    public ApiResponse<DelegationResponse> getDelegation(
            @PathVariable Long companyId, @PathVariable Long delegationId) {
        return ApiResponse.ok(managementDelegationService.getDelegation(companyId, delegationId));
    }

    /** 撤销授权（移除分组管理员） */
    @DeleteMapping("/{delegationId}")
    public ApiResponse<Void> revokeDelegation(
            @PathVariable Long companyId, @PathVariable Long delegationId) {
        managementDelegationService.revokeDelegation(companyId, delegationId);
        return ApiResponse.ok();
    }
}
