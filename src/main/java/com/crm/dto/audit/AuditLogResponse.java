package com.crm.dto.audit;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 审计日志响应（audit_logs）。
 */
@Data
public class AuditLogResponse {

    /** 日志 ID */
    private Long logId;

    /** 操作用户 ID */
    private Long userId;

    /** 操作用户类型（USER / COMPANY_USER / SYSTEM） */
    private String userType;

    /** 所属企业 ID */
    private Long companyId;

    /** 操作行为 */
    private String action;

    /** 资源类型 */
    private String resourceType;

    /** 资源 ID */
    private String resourceId;

    /** 操作详情（JSON） */
    private Map<String, Object> detail;

    /** 客户端 IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    /** 记录时间 */
    private LocalDateTime createdAt;
}
