package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.resource.GrantPermissionRequest;
import com.crm.dto.resource.ResourcePermissionResponse;
import com.crm.dto.resource.UpdatePermissionRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.ResourcePermissionService;
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
 * 资源权限接口（授权主体支持分组与用户；权限级别 READ / WRITE / OWNER）。
 */
@RestController
@RequestMapping("/api/resources/{resourceId}/permissions")
@RequiredArgsConstructor
public class ResourcePermissionController {

    private final ResourcePermissionService resourcePermissionService;

    /** 资源权限列表 */
    @GetMapping
    public ApiResponse<List<ResourcePermissionResponse>> listPermissions(@PathVariable Long resourceId) {
        return ApiResponse.ok(resourcePermissionService.listPermissions(resourceId));
    }

    /** 分配资源权限给分组 (UC-011) */
    @PostMapping("/groups")
    public ApiResponse<Void> grantToGroup(
            @PathVariable Long resourceId, @Valid @RequestBody GrantPermissionRequest request) {
        resourcePermissionService.grantToGroup(resourceId, SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 分配资源权限给用户 (UC-012) */
    @PostMapping("/users")
    public ApiResponse<Void> grantToUser(
            @PathVariable Long resourceId, @Valid @RequestBody GrantPermissionRequest request) {
        resourcePermissionService.grantToUser(resourceId, SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 修改权限级别 */
    @PutMapping("/{permissionId}")
    public ApiResponse<Void> updatePermission(
            @PathVariable Long resourceId,
            @PathVariable Long permissionId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        resourcePermissionService.updatePermission(resourceId, permissionId, request);
        return ApiResponse.ok();
    }

    /** 撤销资源权限 (UC-016) */
    @DeleteMapping("/{permissionId}")
    public ApiResponse<Void> revokePermission(
            @PathVariable Long resourceId, @PathVariable Long permissionId) {
        resourcePermissionService.revokePermission(resourceId, permissionId);
        return ApiResponse.ok();
    }
}
