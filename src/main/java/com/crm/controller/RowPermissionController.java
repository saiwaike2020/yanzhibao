package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.resource.RowPermissionRuleRequest;
import com.crm.dto.resource.RowPermissionRuleResponse;
import com.crm.dto.resource.UpdateRowPermissionRuleRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.RowPermissionService;
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
 * 数据行级权限规则接口（基于业务对象属性过滤数据行，与资源权限叠加生效）。
 */
@RestController
@RequestMapping("/api/resources/{resourceId}/row-permissions")
@RequiredArgsConstructor
public class RowPermissionController {

    private final RowPermissionService rowPermissionService;

    /** 设置数据行级权限规则 (UC-013) */
    @PostMapping
    public ApiResponse<Void> createRule(
            @PathVariable Long resourceId, @Valid @RequestBody RowPermissionRuleRequest request) {
        rowPermissionService.createRule(resourceId, SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 规则列表 */
    @GetMapping
    public ApiResponse<List<RowPermissionRuleResponse>> listRules(@PathVariable Long resourceId) {
        return ApiResponse.ok(rowPermissionService.listRules(resourceId));
    }

    /** 更新规则 */
    @PutMapping("/{ruleId}")
    public ApiResponse<Void> updateRule(
            @PathVariable Long resourceId,
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateRowPermissionRuleRequest request) {
        rowPermissionService.updateRule(resourceId, ruleId, request);
        return ApiResponse.ok();
    }

    /** 删除规则 */
    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long resourceId, @PathVariable Long ruleId) {
        rowPermissionService.deleteRule(resourceId, ruleId);
        return ApiResponse.ok();
    }
}
