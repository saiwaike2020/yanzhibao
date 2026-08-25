package com.crm.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邀请成员加入企业请求 (UC-006)。
 */
@Data
public class InviteMemberRequest {

    /** 被邀请人手机号（未注册用户将通过短信邀请） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邀请备注（可选） */
    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String note;
}
