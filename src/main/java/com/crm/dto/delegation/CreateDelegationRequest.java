package com.crm.dto.delegation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建管理授权请求（设置分组管理员，UC-008 / UC-015）。
 * 属于角色提权敏感操作，需操作者（企业管理员）手机短信验证码二次校验。
 */
@Data
public class CreateDelegationRequest {

    /** 被管理的分组 ID */
    @NotNull(message = "分组不能为空")
    private Long groupId;

    /** 被授权用户 ID（需为企业成员） */
    @NotNull(message = "被授权用户不能为空")
    private Long granteeUserId;

    /** 操作者（企业管理员）短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
}
