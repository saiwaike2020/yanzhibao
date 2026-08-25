package com.crm.service;

import com.crm.dto.group.AddGroupMemberRequest;
import com.crm.dto.group.GroupMemberResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 分组成员服务（企业管理员或该分组的分组管理员可管理本组成员）。
 */
@Service
public class GroupMemberService {

    /** 添加分组成员 (UC-009) */
    public void addGroupMember(Long groupId, AddGroupMemberRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分组成员列表 */
    public List<GroupMemberResponse> listGroupMembers(Long groupId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 移除分组成员 (UC-016) */
    public void removeGroupMember(Long groupId, Long groupMemberId) {
        throw new UnsupportedOperationException("TODO");
    }
}
