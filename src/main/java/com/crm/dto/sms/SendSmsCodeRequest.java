package com.crm.dto.sms;

import com.crm.common.enums.SmsScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码请求。
 */
@Data
public class SendSmsCodeRequest {

    /** 接收验证码的手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 业务场景（REGISTER / LOGIN / BIND_PHONE / ADMIN_ROLE_CHANGE 等） */
    @NotNull(message = "验证码场景不能为空")
    private SmsScene scene;
}
