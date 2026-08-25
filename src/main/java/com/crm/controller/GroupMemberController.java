package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.group.AddGroupMemberRequest;
import com.crm.dto.group.GroupMemberResponse;
import com.crm.service.GroupMemberService;
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
 * 分组成员管理接口（企业管理员或该分组的分组管理员可管理本组成员）。
 */
@RestController
@RequestMapping("/api/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    /** 添加分组成员 (UC-009) */
    @PostMapping
    public ApiResponse<Void> addGroupMember(
            @PathVariable Long groupId, @Valid @RequestBody AddGroupMemberRequest request) {
        groupMemberService.addGroupMember(groupId, request);
        return ApiResponse.ok();
    }

    /** 分组成员列表 */
    @GetMapping
    public ApiResponse<List<GroupMemberResponse>> listGroupMembers(@PathVariable Long groupId) {
        return ApiResponse.ok(groupMemberService.listGroupMembers(groupId));
    }

    /** 移除分组成员 (UC-016) */
    @DeleteMapping("/{groupMemberId}")
    public ApiResponse<Void> removeGroupMember(@PathVariable Long groupId, @PathVariable Long groupMemberId) {
        groupMemberService.removeGroupMember(groupId, groupMemberId);
        return ApiResponse.ok();
    }
}
