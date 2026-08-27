package com.crm.service;

import com.crm.common.enums.AuditScope;
import com.crm.common.enums.SmsScene;
import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
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
import com.crm.entity.AuditLog;
import com.crm.entity.AuditPermission;
import com.crm.entity.StorageQuota;
import com.crm.entity.SysUser;
import com.crm.entity.SystemSetting;
import com.crm.repository.AuditLogRepository;
import com.crm.repository.AuditPermissionRepository;
import com.crm.repository.StorageQuotaRepository;
import com.crm.repository.SysUserRepository;
import com.crm.repository.SystemSettingRepository;
import com.crm.security.SecurityUtils;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final SysUserRepository sysUserRepository;
    private final AuditPermissionRepository auditPermissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final SmsVerificationService smsVerificationService;

    /** 用户列表（分页 / 关键字查询） */
    public PageResponse<UserProfileResponse> listUsers(PageQueryRequest query) {
        PageRequest pageable = PageRequest.of(query.getPage() - 1, query.getSize());
        Page<SysUser> page = sysUserRepository.searchByKeyword(query.getKeyword(), pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toUserProfile).toList(),
                page.getTotalElements(), query.getPage(), query.getSize());
    }

    /** 用户详情 */
    public UserProfileResponse getUserDetail(Long userId) {
        return toUserProfile(findUser(userId));
    }

    /** 禁用用户 */
    @Transactional
    public void disableUser(Long userId) {
        SysUser user = findUser(userId);
        user.setStatus(UserStatus.DISABLED);
        sysUserRepository.save(user);
    }

    /** 恢复用户 */
    @Transactional
    public void restoreUser(Long userId) {
        SysUser user = findUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        sysUserRepository.save(user);
    }

    /** 注销用户 */
    @Transactional
    public void cancelUser(Long userId) {
        SysUser user = findUser(userId);
        user.setStatus(UserStatus.CANCELLED);
        user.setDeletedAt(LocalDateTime.now());
        sysUserRepository.save(user);
    }

    /** 审计人员列表 */
    public List<AuditorResponse> listAuditors() {
        return sysUserRepository.findBySystemRole(SystemRole.AUDITOR).stream()
                .map(u -> toAuditorResponse(u, auditPermissionRepository.findByUserId(u.getUserId()).orElse(null)))
                .toList();
    }

    /** 分配审计人员权限及查看范围（短信验证，UC-019） */
    @Transactional
    public void assignAuditor(AssignAuditorRequest request) {
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        verifyAdminSms(operatorUserId, request.getSmsCode());

        SysUser target = findUser(request.getUserId());
        if (auditPermissionRepository.existsByUserId(target.getUserId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已分配审计权限");
        }

        AuditPermission permission = new AuditPermission();
        permission.setUserId(target.getUserId());
        permission.setAuditScope(request.getAuditScope());
        permission.setGrantedBy(operatorUserId);
        auditPermissionRepository.save(permission);

        target.setSystemRole(SystemRole.AUDITOR);
        sysUserRepository.save(target);
    }

    /** 调整审计人员权限 / 查看范围（短信验证） */
    @Transactional
    public void updateAuditor(Long userId, UpdateAuditorRequest request) {
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        verifyAdminSms(operatorUserId, request.getSmsCode());

        AuditPermission permission = auditPermissionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITOR_NOT_FOUND));
        permission.setAuditScope(request.getAuditScope());
        permission.setGrantedBy(operatorUserId);
        auditPermissionRepository.save(permission);
    }

    /** 撤销审计角色 */
    @Transactional
    public void revokeAuditor(Long userId) {
        AuditPermission permission = auditPermissionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITOR_NOT_FOUND));
        auditPermissionRepository.delete(permission);

        SysUser user = findUser(userId);
        user.setSystemRole(SystemRole.NONE);
        sysUserRepository.save(user);
    }

    /** 查看全部审计日志 */
    public PageResponse<AuditLogResponse> listAllAuditLogs(AuditLogQueryRequest query) {
        List<AuditLog> filtered = auditLogRepository.findAll().stream()
                .filter(log -> matchQuery(log, query))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .toList();
        int from = (query.getPage() - 1) * query.getSize();
        List<AuditLogResponse> items = filtered.stream()
                .skip(from)
                .limit(query.getSize())
                .map(this::toAuditLogResponse)
                .toList();
        return PageResponse.of(items, filtered.size(), query.getPage(), query.getSize());
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

    /** 系统管理员短信二次验证（UC-019，场景 AUDITOR_ASSIGN） */
    private void verifyAdminSms(Long operatorUserId, String smsCode) {
        SysUser operator = findUser(operatorUserId);
        if (!StringUtils.hasText(operator.getPhone())) {
            throw new BusinessException(ErrorCode.SMS_MISSING_PHONE);
        }
        smsVerificationService.verifyCode(operator.getPhone(), SmsScene.AUDITOR_ASSIGN, smsCode);
    }

    /** 查询用户，不存在抛异常 */
    private SysUser findUser(Long userId) {
        return sysUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    /** 组装用户资料响应 */
    private UserProfileResponse toUserProfile(SysUser user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUserNo(user.getUserNo());
        response.setPhoneMasked(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus());
        response.setSystemRole(user.getSystemRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    /** 组装审计人员响应 */
    private AuditorResponse toAuditorResponse(SysUser user, AuditPermission permission) {
        AuditorResponse response = new AuditorResponse();
        response.setUserId(user.getUserId());
        response.setUserNo(user.getUserNo());
        response.setNickname(user.getNickname());
        response.setPhoneMasked(maskPhone(user.getPhone()));
        if (permission != null) {
            response.setAuditScope(permission.getAuditScope());
            response.setScopeDetails(permission.getScopeDetails());
            response.setGrantedBy(permission.getGrantedBy());
            response.setStatus(1);
            response.setCreatedAt(permission.getCreatedAt());
        }
        return response;
    }

    /** 组装审计日志响应 */
    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setLogId(log.getLogId());
        response.setUserId(log.getUserId());
        response.setUserType(log.getUserType());
        response.setCompanyId(log.getCompanyId());
        response.setAction(log.getAction());
        response.setResourceType(log.getResourceType());
        response.setResourceId(log.getResourceId());
        response.setDetail(log.getDetail());
        response.setIpAddress(log.getIpAddress());
        response.setUserAgent(log.getUserAgent());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    /** 审计日志查询条件匹配 */
    private boolean matchQuery(AuditLog log, AuditLogQueryRequest q) {
        if (q.getUserId() != null && !q.getUserId().equals(log.getUserId())) {
            return false;
        }
        if (q.getCompanyId() != null && !q.getCompanyId().equals(log.getCompanyId())) {
            return false;
        }
        if (StringUtils.hasText(q.getAction()) && !q.getAction().equals(log.getAction())) {
            return false;
        }
        if (q.getStartTime() != null && log.getCreatedAt() != null && log.getCreatedAt().isBefore(q.getStartTime())) {
            return false;
        }
        if (q.getEndTime() != null && log.getCreatedAt() != null && log.getCreatedAt().isAfter(q.getEndTime())) {
            return false;
        }
        return true;
    }

    /** 手机号掩码：138****1234 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

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
