package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.group.CreateGroupRequest;
import com.crm.dto.group.GroupResponse;
import com.crm.dto.group.UpdateGroupRequest;
import com.crm.service.GroupService;
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
 * 企业分组管理接口（创建、编辑、删除、查询分组；当前仅支持一级分组）。
 */
@RestController
@RequestMapping("/api/companies/{companyId}/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /** 创建一级分组（仅企业管理员，parentGroupId 为空，UC-007） */
    @PostMapping
    public ApiResponse<GroupResponse> createGroup(
            @PathVariable Long companyId, @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.ok(groupService.createGroup(companyId, request));
    }

    /** 分组列表 */
    @GetMapping
    public ApiResponse<List<GroupResponse>> listGroups(@PathVariable Long companyId) {
        return ApiResponse.ok(groupService.listGroups(companyId));
    }

    /** 分组详情 */
    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable Long companyId, @PathVariable Long groupId) {
        return ApiResponse.ok(groupService.getGroup(companyId, groupId));
    }

    /** 编辑分组（仅企业管理员） */
    @PutMapping("/{groupId}")
    public ApiResponse<GroupResponse> updateGroup(
            @PathVariable Long companyId,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ApiResponse.ok(groupService.updateGroup(companyId, groupId, request));
    }

    /** 删除分组（仅企业管理员） */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long companyId, @PathVariable Long groupId) {
        groupService.deleteGroup(companyId, groupId);
        return ApiResponse.ok();
    }
}
