package com.crm.service;

import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.audit.AuditUserInfoResponse;
import com.crm.dto.common.PageResponse;
import org.springframework.stereotype.Service;

/**
 * 审计与客服服务。
 * 审计人员按系统管理员分配的查看范围查询日志；
 * 客服人员验证服务对象后查看用户信息与权限范围内的日志。
 */
@Service
public class AuditService {

    /** 审计人员在其授权范围内查询审计日志 (UC-020) */
    public PageResponse<AuditLogResponse> queryAuditLogs(Long auditorUserId, AuditLogQueryRequest query) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 客服人员验证服务对象后查看用户信息 (UC-021) */
    public AuditUserInfoResponse getUserInfoForService(Long targetUserId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 客服人员查看指定用户权限范围内的日志 (UC-021) */
    public PageResponse<AuditLogResponse> queryUserLogs(Long targetUserId, AuditLogQueryRequest query) {
        throw new UnsupportedOperationException("TODO");
    }
}
