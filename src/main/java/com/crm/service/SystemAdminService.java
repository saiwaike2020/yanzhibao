package com.crm.service;

import com.crm.dto.admin.AssignAuditorRequest;
import com.crm.dto.admin.AuditorResponse;
import com.crm.dto.admin.SystemSettingRequest;
import com.crm.dto.admin.SystemSettingResponse;
import com.crm.dto.admin.UpdateAuditorRequest;
import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.user.UserProfileResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 系统管理服务（系统管理员专属，系统级最高权限）。
 */
@Service
public class SystemAdminService {

    /** 用户列表（分页 / 关键字查询） */
    public PageResponse<UserProfileResponse> listUsers(PageQueryRequest query) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 用户详情 */
    public UserProfileResponse getUserDetail(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 禁用用户 */
    public void disableUser(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 恢复用户 */
    public void restoreUser(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 注销用户 */
    public void cancelUser(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 审计人员列表 */
    public List<AuditorResponse> listAuditors() {
        throw new UnsupportedOperationException("TODO");
    }

    /** 分配审计人员权限及查看范围（短信验证，UC-019） */
    public void assignAuditor(AssignAuditorRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 调整审计人员权限 / 查看范围（短信验证） */
    public void updateAuditor(Long userId, UpdateAuditorRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 撤销审计角色 */
    public void revokeAuditor(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 查看全部审计日志 */
    public PageResponse<AuditLogResponse> listAllAuditLogs(AuditLogQueryRequest query) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 查询系统参数 */
    public SystemSettingResponse getSystemSetting(String key) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 配置系统参数 */
    public void updateSystemSetting(SystemSettingRequest request) {
        throw new UnsupportedOperationException("TODO");
    }
}
