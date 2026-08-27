package com.crm.service;

import com.crm.common.enums.AuthType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.user.AccountSecurityResponse;
import com.crm.dto.user.UpdateProfileRequest;
import com.crm.dto.user.UserProfileResponse;
import com.crm.entity.SysUser;
import com.crm.entity.UserAuth;
import com.crm.repository.SysUserRepository;
import com.crm.repository.UserAuthRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户个人中心服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository sysUserRepository;
    private final UserAuthRepository userAuthRepository;

    /** 获取当前登录用户信息 */
    public UserProfileResponse getCurrentUser(Long userId) {
        return toProfile(findUser(userId));
    }

    /** 更新个人资料（昵称、头像等） */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = findUser(userId);
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        sysUserRepository.save(user);
        return toProfile(user);
    }

    /** 账号安全信息（已绑定认证方式、手机号掩码、是否设置密码） */
    public AccountSecurityResponse getAccountSecurity(Long userId) {
        SysUser user = findUser(userId);
        List<UserAuth> auths = userAuthRepository.findByUserId(userId);

        AccountSecurityResponse response = new AccountSecurityResponse();
        response.setPhoneMasked(maskPhone(user.getPhone()));
        response.setBoundAuthTypes(auths.stream().map(UserAuth::getAuthType).toList());
        response.setHasPassword(auths.stream()
                .anyMatch(a -> a.getAuthType() == AuthType.PHONE && StringUtils.hasText(a.getCredential())));
        return response;
    }

    /** 查看指定用户公开信息 */
    public UserProfileResponse getUserById(Long userId) {
        return toProfile(findUser(userId));
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    private SysUser findUser(Long userId) {
        return sysUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private UserProfileResponse toProfile(SysUser user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUserNo(user.getUserNo());
        response.setPhoneMasked(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus());
        response.setSystemRole(user.getSystemRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    /** 手机号掩码：138****1234 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

