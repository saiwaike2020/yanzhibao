package com.crm.dto.company;

import com.crm.common.enums.CompanyMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 变更企业成员角色请求（设为管理员 / 取消管理员，UC-022）。
 * 属于高危敏感权限变更，需操作者手机短信验证码二次校验。
 */
@Data
public class ChangeMemberRoleRequest {

    /** 目标角色：ADMIN（设为管理员）或 MEMBER（取消管理员） */
    @NotNull(message = "目标角色不能为空")
    private CompanyMemberRole role;

    /** 操作者（企业管理员/所有者）短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
}
