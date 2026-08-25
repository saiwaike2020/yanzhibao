package com.crm.service;

import com.crm.common.enums.SmsScene;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.sms.SendSmsCodeRequest;
import com.crm.entity.SmsVerification;
import com.crm.repository.SmsVerificationRepository;
import com.crm.repository.SysUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 短信验证码服务。
 *
 * <p>职责：发送验证码（5 分钟有效、同一手机号 60 秒限频）、
 * 校验验证码（存在、未过期、未消耗、尝试次数未超限、匹配正确）并标记已消耗。
 *
 * <p><b>Mock 通道说明</b>：当前固定发送 {@value #MOCK_CODE}，后续接入真实短信网关时
 * 只需替换 {@link #sendCode(String, String)} 的实现即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    /** Mock 验证码：真实短信通道接入前固定为 000000 */
    public static final String MOCK_CODE = "000000";

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    /** 最大尝试校验次数，超过后当前验证码失效 */
    private static final int MAX_ATTEMPTS = 5;

    /** 同一手机号同一场景发送最小间隔（秒） */
    private static final long SEND_INTERVAL_SECONDS = 60;

    private final SmsVerificationRepository smsVerificationRepository;
    private final SysUserRepository sysUserRepository;

    /** 发送短信验证码（客户端 IP 可选） */
    @Transactional
    public void sendVerificationCode(SendSmsCodeRequest request) {
        sendVerificationCode(request, null);
    }

    /** 发送短信验证码 */
    @Transactional
    public void sendVerificationCode(SendSmsCodeRequest request, String ipAddress) {
        String phone = request.getPhone();
        SmsScene scene = request.getScene();

        // 注册场景：手机号已注册则提示直接登录
        if (scene == SmsScene.REGISTER && sysUserRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // 重置密码场景：手机号必须已注册，否则提示未注册（忘记密码 UC-025）
        if (scene == SmsScene.RESET_PWD && !sysUserRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_REGISTERED);
        }

        // 60 秒限频：同一手机号同一场景短时间内仅允许发送一次
        LocalDateTime now = LocalDateTime.now();
        long recentCount = smsVerificationRepository.countByPhoneAndSceneAndCreatedAtAfter(
                phone, scene, now.minusSeconds(SEND_INTERVAL_SECONDS));
        if (recentCount > 0) {
            throw new BusinessException(ErrorCode.SMS_SEND_TOO_FREQUENT);
        }

        // 生成并“发送”验证码（当前为 Mock，恒为 000000）
        String code = MOCK_CODE;
        sendCode(phone, code);

        SmsVerification verification = new SmsVerification();
        verification.setPhone(phone);
        verification.setScene(scene);
        verification.setCodeHash(hash(code));
        verification.setExpiredAt(now.plusMinutes(CODE_EXPIRE_MINUTES));
        verification.setAttempts(0);
        verification.setIpAddress(ipAddress);
        smsVerificationRepository.save(verification);

        log.info("验证码已生成并保存: phone={}, scene={}, 有效期={}分钟", phone, scene, CODE_EXPIRE_MINUTES);
    }

    /**
     * 校验短信验证码，校验通过后标记为已消耗。
     *
     * @param phone 手机号
     * @param scene 场景
     * @param code  用户输入的验证码
     * @throws BusinessException 校验失败时抛出对应错误码
     */
    @Transactional
    public void verifyCode(String phone, SmsScene scene, String code) {
        SmsVerification verification = smsVerificationRepository
                .findTopByPhoneAndSceneOrderByCreatedAtDesc(phone, scene)
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_CODE_INVALID));

        if (verification.getUsedAt() != null) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        if (verification.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED);
        }
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT);
        }
        if (!verification.getCodeHash().equals(hash(code))) {
            verification.setAttempts(verification.getAttempts() + 1);
            smsVerificationRepository.save(verification);
            if (verification.getAttempts() >= MAX_ATTEMPTS) {
                throw new BusinessException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT);
            }
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }

        // 校验通过，标记已消耗
        verification.setUsedAt(LocalDateTime.now());
        smsVerificationRepository.save(verification);
    }

    /**
     * Mock 短信发送通道：当前不真正发短信，仅打印日志。
     * 后续接入真实短信网关（如阿里云 / 腾讯云短信）时实现此方法即可。
     */
    private void sendCode(String phone, String code) {
        log.info("[Mock 短信通道] 向手机号 {} 发送验证码: {}", phone, code);
    }

    /** SHA-256 摘要（hex），用于存储验证码哈希 */
    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}

