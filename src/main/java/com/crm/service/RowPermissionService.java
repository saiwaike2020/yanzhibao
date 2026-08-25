package com.crm.service;

import com.crm.dto.resource.RowPermissionRuleRequest;
import com.crm.dto.resource.RowPermissionRuleResponse;
import com.crm.dto.resource.UpdateRowPermissionRuleRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 数据行级权限规则服务（基于业务对象属性过滤数据行，与资源权限叠加生效）。
 */
@Service
public class RowPermissionService {

    /** 设置数据行级权限规则 (UC-013) */
    public void createRule(Long resourceId, Long operatorUserId, RowPermissionRuleRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 规则列表 */
    public List<RowPermissionRuleResponse> listRules(Long resourceId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 更新规则 */
    public void updateRule(Long resourceId, Long ruleId, UpdateRowPermissionRuleRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 删除规则 */
    public void deleteRule(Long resourceId, Long ruleId) {
        throw new UnsupportedOperationException("TODO");
    }
}
