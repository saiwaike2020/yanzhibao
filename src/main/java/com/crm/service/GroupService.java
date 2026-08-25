package com.crm.service;

import com.crm.dto.group.CreateGroupRequest;
import com.crm.dto.group.GroupResponse;
import com.crm.dto.group.UpdateGroupRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 企业分组服务（仅企业管理员可创建 / 编辑 / 删除分组）。
 */
@Service
public class GroupService {

    /** 创建一级分组（仅企业管理员，UC-007） */
    public GroupResponse createGroup(Long companyId, CreateGroupRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分组列表 */
    public List<GroupResponse> listGroups(Long companyId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分组详情 */
    public GroupResponse getGroup(Long companyId, Long groupId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 编辑分组（仅企业管理员） */
    public GroupResponse updateGroup(Long companyId, Long groupId, UpdateGroupRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 删除分组（仅企业管理员） */
    public void deleteGroup(Long companyId, Long groupId) {
        throw new UnsupportedOperationException("TODO");
    }
}
