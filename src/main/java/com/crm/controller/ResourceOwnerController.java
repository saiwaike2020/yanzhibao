package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.resource.GrantOwnershipRequest;
import com.crm.dto.resource.ResourceOwnerResponse;
import com.crm.dto.resource.TransferOwnershipRequest;
import com.crm.dto.resource.UpdateOwnershipValidityRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.ResourceOwnerService;
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
 * 资源所有权管理接口（多所有者、转让、有效期控制）。
 *
 * <p>对应需求 v3：6.5.4 所有权管理、UC-023 所有权转让、UC-024 设置所有权有效期。
 */
@RestController
@RequestMapping("/api/resources/{resourceId}/owners")
@RequiredArgsConstructor
public class ResourceOwnerController {

    private final ResourceOwnerService resourceOwnerService;

    /** 资源所有者列表（含有效期） */
    @GetMapping
    public ApiResponse<List<ResourceOwnerResponse>> listOwners(@PathVariable Long resourceId) {
        return ApiResponse.ok(resourceOwnerService.listOwners(resourceId));
    }

    /** 登记新所有者（起始可用日期必填，过期时间可选，为空则一直有效） */
    @PostMapping
    public ApiResponse<ResourceOwnerResponse> addOwner(
            @PathVariable Long resourceId, @Valid @RequestBody GrantOwnershipRequest request) {
        return ApiResponse.ok(resourceOwnerService.addOwner(resourceId, SecurityUtils.getCurrentUserId(), request));
    }

    /** 所有权转让 (UC-023)：转让后原所有者失去所有权，接收方成为新所有者 */
    @PostMapping("/transfer")
    public ApiResponse<ResourceOwnerResponse> transferOwnership(
            @PathVariable Long resourceId, @Valid @RequestBody TransferOwnershipRequest request) {
        return ApiResponse.ok(
                resourceOwnerService.transferOwnership(resourceId, SecurityUtils.getCurrentUserId(), request));
    }

    /** 调整所有权有效期 (UC-024) */
    @PutMapping("/{ownershipId}")
    public ApiResponse<ResourceOwnerResponse> updateValidity(
            @PathVariable Long resourceId,
            @PathVariable Long ownershipId,
            @Valid @RequestBody UpdateOwnershipValidityRequest request) {
        return ApiResponse.ok(
                resourceOwnerService.updateValidity(resourceId, ownershipId, SecurityUtils.getCurrentUserId(), request));
    }

    /** 撤销所有权（停用 resource_owners 记录） */
    @DeleteMapping("/{ownershipId}")
    public ApiResponse<Void> revokeOwnership(@PathVariable Long resourceId, @PathVariable Long ownershipId) {
        resourceOwnerService.revokeOwnership(resourceId, SecurityUtils.getCurrentUserId(), ownershipId);
        return ApiResponse.ok();
    }
}