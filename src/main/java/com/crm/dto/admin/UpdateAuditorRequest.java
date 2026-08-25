package com.crm.dto.admin;

import com.crm.common.enums.AuditScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整审计人员权限 / 查看范围请求（短信验证）。
 */
@Data
public class UpdateAuditorRequest {

    /** 新的审计日志查看范围 */
    @NotNull(message = "审计范围不能为空")
    private AuditScope auditScope;

    /** 操作者（系统管理员）短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
}
