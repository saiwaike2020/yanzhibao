package com.crm.service;

import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.admin.AssignAuditorRequest;
import com.crm.dto.admin.AuditorResponse;
import com.crm.dto.admin.StorageQuotaRequest;
import com.crm.dto.admin.StorageQuotaResponse;
import com.crm.dto.admin.SystemSettingRequest;
import com.crm.dto.admin.SystemSettingResponse;
import com.crm.dto.admin.UpdateAuditorRequest;
import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.user.UserProfileResponse;
import com.crm.entity.StorageQuota;
import com.crm.entity.SystemSetting;
import com.crm.repository.StorageQuotaRepository;
import com.crm.repository.SystemSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统管理服务（系统管理员专属，系统级最高权限）。
 *
 * <p>已实现：系统参数查询 / 配置，含存储配额上限调节（UC-030）；
 * 其余功能（用户管理、审计人员分配、审计日志查看）为 TODO 待后续实现。
 */
@Service
@RequiredArgsConstructor
public class SystemAdminService {

    /** 存储配额参数键前缀（storage.quota.*） */
    private static final String QUOTA_KEY_PREFIX = "storage.quota.";

    private final SystemSettingRepository systemSettingRepository;
    private final StorageQuotaRepository storageQuotaRepository;

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
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
        return toResponse(setting);
    }

    /** 配置系统参数（存储配额等，UC-030）；存储配额值校验为正整数（字节） */
    @Transactional
    public void updateSystemSetting(SystemSettingRequest request) {
        String key = request.getKey();
        String value = request.getValue().trim();

        // 存储配额参数（storage.quota.*）：值必须为正整数
        if (key.startsWith(QUOTA_KEY_PREFIX)) {
            validateQuotaValue(value);
        }

        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }

    /** 设置 / 调整个体存储配额（UC-031），优先于全局默认配额 */
    @Transactional
    public void setStorageQuota(StorageQuotaRequest request, Long operatorUserId) {
        if (request.getQuotaBytes() == null || request.getQuotaBytes() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUOTA_VALUE);
        }
        StorageQuota quota = storageQuotaRepository
                .findByQuotaTypeAndSubjectId(request.getQuotaType(), request.getSubjectId())
                .orElseGet(() -> {
                    StorageQuota q = new StorageQuota();
                    q.setQuotaType(request.getQuotaType());
                    q.setSubjectId(request.getSubjectId());
                    return q;
                });
        quota.setQuotaBytes(request.getQuotaBytes());
        quota.setUpdatedBy(operatorUserId);
        storageQuotaRepository.save(quota);
    }

    /** 查询个体存储配额 */
    public StorageQuotaResponse getStorageQuota(com.crm.common.enums.OwnerType quotaType, Long subjectId) {
        StorageQuota quota = storageQuotaRepository.findByQuotaTypeAndSubjectId(quotaType, subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_QUOTA_NOT_FOUND));
        return toStorageQuotaResponse(quota);
    }

    /** 移除个体存储配额（恢复使用全局默认配额） */
    @Transactional
    public void removeStorageQuota(com.crm.common.enums.OwnerType quotaType, Long subjectId) {
        StorageQuota quota = storageQuotaRepository.findByQuotaTypeAndSubjectId(quotaType, subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_QUOTA_NOT_FOUND));
        storageQuotaRepository.delete(quota);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 存储配额值校验：必须为正整数 */
    private void validateQuotaValue(String value) {
        try {
            if (Long.parseLong(value) <= 0) {
                throw new BusinessException(ErrorCode.INVALID_QUOTA_VALUE);
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_QUOTA_VALUE);
        }
    }

    private SystemSettingResponse toResponse(SystemSetting setting) {
        SystemSettingResponse response = new SystemSettingResponse();
        response.setKey(setting.getSettingKey());
        response.setValue(setting.getSettingValue());
        return response;
    }

    private StorageQuotaResponse toStorageQuotaResponse(StorageQuota quota) {
        StorageQuotaResponse response = new StorageQuotaResponse();
        response.setQuotaType(quota.getQuotaType());
        response.setSubjectId(quota.getSubjectId());
        response.setQuotaBytes(quota.getQuotaBytes());
        response.setUpdatedBy(quota.getUpdatedBy());
        response.setUpdatedAt(quota.getUpdatedAt());
        return response;
    }
}
