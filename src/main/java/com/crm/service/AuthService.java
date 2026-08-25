package com.crm.service;

import com.crm.common.enums.AuthType;
import com.crm.common.enums.SmsScene;
import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
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
import com.crm.entity.SysUser;
import com.crm.entity.UserAuth;
import com.crm.repository.SysUserRepository;
import com.crm.repository.UserAuthRepository;
import com.crm.security.LoginUser;
import com.crm.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务（注册 / 登录 / 微信扫码 / 账号绑定与解绑 / 密码设置）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final UserAuthRepository userAuthRepository;
    private final SmsVerificationService smsVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    /**
     * 手机号 + 短信验证码注册 (UC-001)，成功后返回 JWT Token。
     *
     * <p>流程（设计文档 4.4.1）：
     * 1. 校验手机号未注册；
     * 2. 校验短信验证码（存在、未过期、未消耗、尝试次数未超限、匹配正确），通过后标记已消耗；
     * 3. 创建用户账号（sys_users 主表写入 phone）；
     * 4. 创建手机号认证记录（user_auths，密码 bcrypt 加密存储）；
     * 5. 生成 JWT Token 返回。
     */
    @Transactional
    public AuthTokenResponse registerByPhone(PhoneRegisterRequest request) {
        String phone = request.getPhone();

        // 1. 手机号未注册校验
        if (sysUserRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // 2. 短信验证码校验（通过后标记为已消耗）
        smsVerificationService.verifyCode(phone, SmsScene.REGISTER, request.getSmsCode());

        // 3. 创建用户账号
        SysUser user = new SysUser();
        user.setUserNo(generateUserNo());
        user.setPhone(phone);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : maskPhone(phone));
        user.setAvatarUrl(request.getAvatarUrl());
        user.setStatus(UserStatus.ACTIVE);
        user.setSystemRole(SystemRole.NONE);
        sysUserRepository.save(user);

        // 4. 创建手机号认证记录（密码 bcrypt 加密）
        UserAuth auth = new UserAuth();
        auth.setUserId(user.getUserId());
        auth.setAuthType(AuthType.PHONE);
        auth.setIdentifier(phone);
        auth.setCredential(passwordEncoder.encode(request.getPassword()));
        auth.setVerifiedAt(LocalDateTime.now());
        auth.setStatus(1);
        userAuthRepository.save(auth);

        log.info("手机号验证码注册成功: userId={}, userNo={}, phone={}",
                user.getUserId(), user.getUserNo(), maskPhone(phone));

        // 5. 生成 JWT Token 返回
        return buildAuthToken(user);
    }

    /** 微信快速注册 (UC-002)，成功后返回 JWT Token */
    public AuthTokenResponse registerByWechat(WechatRegisterRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 手机号 + 密码登录 (UC-003) */
    public AuthTokenResponse loginByPhonePassword(PhonePasswordLoginRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 生成微信扫码登录二维码（含一次性 state） */
    public WechatQrcodeResponse createWechatLoginQrcode() {
        throw new UnsupportedOperationException("TODO");
    }

    /** 微信扫码登录回调 (UC-004)；未绑定则进入绑定/注册流程 */
    public AuthTokenResponse loginByWechatCallback(WechatLoginCallbackRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 已登录用户绑定微信 */
    public void bindWechat(Long userId, BindWechatRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 已登录用户绑定 / 更换手机号（短信验证码校验） */
    public void bindPhone(Long userId, BindPhoneRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 解绑认证方式（至少保留一种登录方式） */
    public void unbindAuth(Long userId, UnbindAuthRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 设置 / 修改登录密码 */
    public void setPassword(Long userId, SetPasswordRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * 忘记密码：手机号 + 短信验证码重置密码（UC-025 / 设计文档 4.7）。
     *
     * <p>流程：
     * 1. 校验手机号已注册且账号状态正常（ACTIVE）；
     * 2. 校验短信验证码（RESET_PWD 场景），通过后标记为已消耗；
     * 3. 查找该手机号对应的 PHONE 认证记录，将密码重新 bcrypt 加密后更新。
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = request.getPhone();

        // 1. 手机号已注册 + 账号状态校验
        SysUser user = sysUserRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_REGISTERED));
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.PHONE_NOT_REGISTERED);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getStatus() == UserStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_CANCELLED);
        }

        // 2. 短信验证码校验（通过后标记为已消耗）
        smsVerificationService.verifyCode(phone, SmsScene.RESET_PWD, request.getSmsCode());

        // 3. 更新手机号认证记录的密码哈希（bcrypt）
        UserAuth auth = userAuthRepository.findByAuthTypeAndIdentifier(AuthType.PHONE, phone)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PHONE_NOT_REGISTERED, "该手机号未绑定密码登录方式，无法重置密码"));
        auth.setCredential(passwordEncoder.encode(request.getNewPassword()));
        auth.setVerifiedAt(LocalDateTime.now());
        userAuthRepository.save(auth);

        log.info("密码重置成功: userId={}, userNo={}, phone={}",
                user.getUserId(), user.getUserNo(), maskPhone(phone));
    }

    /** 生成用户编号：USR + 时间戳(毫秒) + 3 位随机数 */
    private String generateUserNo() {
        for (int i = 0; i < 10; i++) {
            String userNo = "USR" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (!sysUserRepository.existsByUserNo(userNo)) {
                return userNo;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户编号生成失败，请重试");
    }

    /** 组装 JWT Token 响应 */
    private AuthTokenResponse buildAuthToken(SysUser user) {
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUserNo(), user.getPhone(), user.getSystemRole());
        AuthTokenResponse response = new AuthTokenResponse();
        response.setToken(jwtTokenProvider.generateToken(loginUser));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        response.setUserId(user.getUserId());
        response.setUserNo(user.getUserNo());
        response.setPhoneMasked(maskPhone(user.getPhone()));
        response.setSystemRole(user.getSystemRole());
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
