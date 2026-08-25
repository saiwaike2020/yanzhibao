package com.crm.dto.admin;

import com.crm.common.enums.AuditScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统管理员分配审计人员权限请求 (UC-019)。
 * 属于敏感系统权限配置，需操作者手机短信验证码二次校验。
 */
@Data
public class AssignAuditorRequest {

    /** 目标用户 ID */
    @NotNull(message = "目标用户不能为空")
    private Long userId;

    /** 审计日志查看范围 */
    @NotNull(message = "审计范围不能为空")
    private AuditScope auditScope;

    /** 操作者（系统管理员）短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
}
