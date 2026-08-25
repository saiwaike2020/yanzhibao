package com.crm.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置 / 修改登录密码请求。
 */
@Data
public class SetPasswordRequest {

    /** 新密码：至少 8 位，需包含字母和数字 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String newPassword;

    /** 原密码（修改密码时必填；首次设置密码时为空） */
    private String oldPassword;
}
