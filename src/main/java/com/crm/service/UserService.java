package com.crm.service;

import com.crm.dto.user.AccountSecurityResponse;
import com.crm.dto.user.UpdateProfileRequest;
import com.crm.dto.user.UserProfileResponse;
import org.springframework.stereotype.Service;

/**
 * 用户个人中心服务。
 */
@Service
public class UserService {

    /** 获取当前登录用户信息 */
    public UserProfileResponse getCurrentUser(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 更新个人资料（昵称、头像等） */
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 账号安全信息（已绑定认证方式、手机号掩码、是否设置密码） */
    public AccountSecurityResponse getAccountSecurity(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 查看指定用户公开信息 */
    public UserProfileResponse getUserById(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }
}
