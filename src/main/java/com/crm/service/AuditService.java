package com.crm.service;

import com.crm.common.enums.AuditScope;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.audit.AuditUserInfoResponse;
import com.crm.dto.common.PageResponse;
import com.crm.entity.AuditLog;
import com.crm.entity.AuditPermission;
import com.crm.entity.SysUser;
import com.crm.repository.AuditLogRepository;
import com.crm.repository.AuditPermissionRepository;
import com.crm.repository.SysUserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 审计与客服服务。
 *
 * <p>审计人员按系统管理员分配的查看范围（{@code audit_scope} + {@code scope_details}）查询日志；
 * 客服人员验证服务对象后查看用户信息与权限范围内的日志。
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditPermissionRepository auditPermissionRepository;
    private final SysUserRepository sysUserRepository;

    /** 审计人员在其授权范围内查询审计日志 (UC-020) */
    public PageResponse<AuditLogResponse> queryAuditLogs(Long auditorUserId, AuditLogQueryRequest query) {
        AuditPermission permission = auditPermissionRepository.findByUserId(auditorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUDIT_PERMISSION));
        if (isExpired(permission)) {
            throw new BusinessException(ErrorCode.NO_AUDIT_PERMISSION);
        }

        List<AuditLog> filtered = auditLogRepository.findAll().stream()
                .filter(log -> canAuditorRead(permission, log))
                .filter(log -> matchQuery(log, query))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .toList();

        return pageOf(filtered, query);
    }

    /** 客服人员验证服务对象后查看用户信息 (UC-021) */
    public AuditUserInfoResponse getUserInfoForService(Long targetUserId) {
        SysUser user = sysUserRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        return toUserInfo(user);
    }

    /** 客服人员查看指定用户权限范围内的日志 (UC-021) */
    public PageResponse<AuditLogResponse> queryUserLogs(Long targetUserId, AuditLogQueryRequest query) {
        List<AuditLog> filtered = auditLogRepository.findByUserId(targetUserId).stream()
                .filter(log -> matchQuery(log, query))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .toList();
        return pageOf(filtered, query);
    }


    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 分页封装 */
    private PageResponse<AuditLogResponse> pageOf(List<AuditLog> logs, AuditLogQueryRequest query) {
        int from = (query.getPage() - 1) * query.getSize();
        List<AuditLogResponse> items = logs.stream()
                .skip(from)
                .limit(query.getSize())
                .map(this::toResponse)
                .toList();
        return PageResponse.of(items, logs.size(), query.getPage(), query.getSize());
    }

    /** 审计人员能否查看某条日志（核心：audit_scope + scope_details 范围判断） */
    private boolean canAuditorRead(AuditPermission ap, AuditLog log) {
        AuditScope scope = ap.getAuditScope();
        if (scope == AuditScope.ALL) {
            // 查看全部日志，但不含系统用户敏感操作日志
            return !"SYSTEM".equals(log.getUserType());
        }
        if (scope == AuditScope.REGULAR_USERS) {
            // 仅普通用户日志（user_type=USER）
            return "USER".equals(log.getUserType());
        }
        if (scope == AuditScope.ENTERPRISE_USERS) {
            // 仅企业用户日志（user_type=COMPANY_USER），且 allowed_company_ids 未限定或包含该企业
            if (!"COMPANY_USER".equals(log.getUserType())) {
                return false;
            }
            Map<String, Object> details = ap.getScopeDetails();
            if (details == null || details.get("allowed_company_ids") == null) {
                return true;
            }
            Object ids = details.get("allowed_company_ids");
            if (ids instanceof List<?> list) {
                return list.stream().map(String::valueOf)
                        .anyMatch(v -> v.equals(String.valueOf(log.getCompanyId())));
            }
            return false;
        }
        return false;
    }

    /** 审计权限是否已过期 */
    private boolean isExpired(AuditPermission ap) {
        return ap.getExpiredAt() != null && ap.getExpiredAt().isBefore(LocalDateTime.now());
    }

    /** 查询条件匹配 */
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

    private AuditLogResponse toResponse(AuditLog log) {
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

    private AuditUserInfoResponse toUserInfo(SysUser user) {
        AuditUserInfoResponse response = new AuditUserInfoResponse();
        response.setUserId(user.getUserId());
        response.setUserNo(user.getUserNo());
        response.setNickname(user.getNickname());
        response.setPhoneMasked(maskPhone(user.getPhone()));
        response.setStatus(user.getStatus());
        response.setSystemRole(user.getSystemRole());
        return response;
    }

    /** 手机号掩码：138****1234 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

