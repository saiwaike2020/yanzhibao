package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.sms.SendSmsCodeRequest;
import com.crm.service.SmsVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码接口（注册、登录、绑定手机号，以及管理员提权 / 降级等敏感操作二次校验）。
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsVerificationController {

    private final SmsVerificationService smsVerificationService;

    /** 发送短信验证码（scene: REGISTER / LOGIN / BIND_PHONE / ADMIN_ROLE_CHANGE / GROUP_MANAGE_GRANT / AUDITOR_ASSIGN） */
    @PostMapping("/verification-code")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody SendSmsCodeRequest request,
                                                  HttpServletRequest httpRequest) {
        smsVerificationService.sendVerificationCode(request, httpRequest.getRemoteAddr());
        return ApiResponse.ok();
    }
}
