package com.crm.controller;

import com.crm.dto.auth.AuthTokenResponse;
import com.crm.dto.auth.BindPhoneRequest;
import com.crm.dto.auth.BindWechatRequest;
import com.crm.dto.auth.PhonePasswordLoginRequest;
import com.crm.dto.auth.PhoneRegisterRequest;
import com.crm.dto.auth.ResetPasswordRequest;
import com.crm.dto.auth.SetPasswordRequest;
import com.crm.dto.auth.UnbindAuthRequest;
import com.crm.dto.auth.WechatLoginCallbackRequest;
import com.crm.dto.auth.WechatQrcodeResponse;
import com.crm.dto.auth.WechatRegisterRequest;
import com.crm.dto.common.ApiResponse;
import com.crm.security.SecurityUtils;
import com.crm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口（注册 / 登录 / 微信扫码 / 账号绑定与解绑 / 密码设置）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 手机号 + 短信验证码注册 (UC-001) */
    @PostMapping("/register/phone")
    public ApiResponse<AuthTokenResponse> registerByPhone(@Valid @RequestBody PhoneRegisterRequest request) {
        return ApiResponse.ok(authService.registerByPhone(request));
    }

    /** 微信快速注册 (UC-002) */
    @PostMapping("/register/wechat")
    public ApiResponse<AuthTokenResponse> registerByWechat(@Valid @RequestBody WechatRegisterRequest request) {
        return ApiResponse.ok(authService.registerByWechat(request));
    }

    /** 手机号 + 密码登录 (UC-003) */
    @PostMapping("/login/phone")
    public ApiResponse<AuthTokenResponse> loginByPhonePassword(@Valid @RequestBody PhonePasswordLoginRequest request) {
        return ApiResponse.ok(authService.loginByPhonePassword(request));
    }

    /** 生成微信扫码登录二维码 */
    @GetMapping("/login/wechat/qrcode")
    public ApiResponse<WechatQrcodeResponse> createWechatLoginQrcode() {
        return ApiResponse.ok(authService.createWechatLoginQrcode());
    }

    /** 微信扫码登录回调 (UC-004) */
    @PostMapping("/login/wechat/callback")
    public ApiResponse<AuthTokenResponse> loginByWechatCallback(@Valid @RequestBody WechatLoginCallbackRequest request) {
        return ApiResponse.ok(authService.loginByWechatCallback(request));
    }

    /** 绑定微信（需登录） */
    @PostMapping("/bind/wechat")
    public ApiResponse<Void> bindWechat(@Valid @RequestBody BindWechatRequest request) {
        authService.bindWechat(SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 绑定 / 更换手机号（需登录，短信验证码校验） */
    @PostMapping("/bind/phone")
    public ApiResponse<Void> bindPhone(@Valid @RequestBody BindPhoneRequest request) {
        authService.bindPhone(SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 解绑认证方式（需登录，至少保留一种登录方式） */
    @PostMapping("/unbind")
    public ApiResponse<Void> unbindAuth(@Valid @RequestBody UnbindAuthRequest request) {
        authService.unbindAuth(SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 设置 / 修改登录密码（需登录） */
    @PostMapping("/password/set")
    public ApiResponse<Void> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.ok();
    }

    /** 忘记密码：手机号 + 短信验证码重置密码（UC-025，无需登录） */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }
}
