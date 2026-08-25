package com.crm.dto.admin;

import com.crm.common.enums.AuditScope;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 审计人员信息响应。
 */
@Data
public class AuditorResponse {

    /** 审计人员用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 昵称 */
    private String nickname;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 审计日志查看范围 */
    private AuditScope auditScope;

    /** 查看范围明细（scope_details，如指定用户/企业范围） */
    private Map<String, Object> scopeDetails;

    /** 授权人用户 ID */
    private Long grantedBy;

    /** 状态（1-正常 0-停用） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
