package com.crm.controller;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.user.AccountSecurityResponse;
import com.crm.dto.user.UpdateProfileRequest;
import com.crm.dto.user.UserProfileResponse;
import com.crm.security.SecurityUtils;
import com.crm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人中心接口（普通用户 / 企业用户 / 系统用户通用）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUser() {
        return ApiResponse.ok(userService.getCurrentUser(SecurityUtils.getCurrentUserId()));
    }

    /** 更新个人资料（昵称、头像等） */
    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }

    /** 账号安全信息（已绑定认证方式、手机号掩码、是否设置密码） */
    @GetMapping("/me/security")
    public ApiResponse<AccountSecurityResponse> getAccountSecurity() {
        return ApiResponse.ok(userService.getAccountSecurity(SecurityUtils.getCurrentUserId()));
    }

    /** 查看指定用户公开信息 */
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserById(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserById(userId));
    }
}
