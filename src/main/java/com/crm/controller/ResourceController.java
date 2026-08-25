package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.resource.CreateResourceRequest;
import com.crm.dto.resource.ResourceResponse;
import com.crm.dto.resource.UpdateResourceRequest;
import com.crm.security.SecurityUtils;
import com.crm.service.ResourceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源管理接口（统一资源模型：资料库 LIBRARY / 文件夹 FOLDER / 文件 FILE）。
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /** 创建资源（校验父级写权限，UC-010） */
    @PostMapping
    public ApiResponse<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest request) {
        return ApiResponse.ok(resourceService.createResource(SecurityUtils.getCurrentUserId(), request));
    }

    /** 资源树（按权限过滤） */
    @GetMapping("/tree")
    public ApiResponse<List<ResourceResponse>> getResourceTree(
            @RequestParam(value = "rootResourceId", required = false) Long rootResourceId) {
        return ApiResponse.ok(resourceService.getResourceTree(rootResourceId));
    }

    /** 资源详情 */
    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceResponse> getResource(@PathVariable Long resourceId) {
        return ApiResponse.ok(resourceService.getResource(resourceId));
    }

    /** 更新资源（重命名、移动，需父级写权限） */
    @PutMapping("/{resourceId}")
    public ApiResponse<ResourceResponse> updateResource(
            @PathVariable Long resourceId, @Valid @RequestBody UpdateResourceRequest request) {
        return ApiResponse.ok(resourceService.updateResource(resourceId, request));
    }

    /** 删除 / 归档资源（需 OWNER 权限） */
    @DeleteMapping("/{resourceId}")
    public ApiResponse<Void> deleteResource(@PathVariable Long resourceId) {
        resourceService.deleteResource(resourceId);
        return ApiResponse.ok();
    }
}
