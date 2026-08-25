package com.crm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.AuthType;
import com.crm.entity.UserAuth;
import com.crm.repository.UserAuthRepository;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 忘记密码 / 手机号验证码重置密码（UC-025 / 设计文档 4.7）集成测试。
 *
 * <p>依赖本地 PostgreSQL（docker compose 启动），短信为 Mock 通道（固定 000000）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 随机生成一个未注册手机号（避免测试数据冲突） */
    private String randomPhone() {
        return "138" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /** 注册一个可用的测试账号（初始密码 abc12345） */
    private void registerUser(String phone) throws Exception {
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 完整重置密码流程：注册 → 发送 RESET_PWD 验证码 → 重置 → 数据库密码已更新 */
    @Test
    void passwordResetFlowShouldSucceed() throws Exception {
        String phone = randomPhone();
        registerUser(phone);

        // 1. 发送重置密码验证码（scene=RESET_PWD）
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"RESET_PWD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 2. 使用固定验证码 000000 重置为新密码 xyz67890
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\",\"newPassword\":\"xyz67890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 3. 数据库中的密码哈希已更新为新密码，旧密码失效
        UserAuth auth = userAuthRepository.findByAuthTypeAndIdentifier(AuthType.PHONE, phone).orElseThrow();
        assertTrue(passwordEncoder.matches("xyz67890", auth.getCredential()));
        assertFalse(passwordEncoder.matches("abc12345", auth.getCredential()));
    }

    /** 未注册手机号发送重置密码验证码应失败（PHONE_NOT_REGISTERED = 1002） */
    @Test
    void sendResetCodeForUnregisteredPhoneShouldFail() throws Exception {
        String phone = randomPhone();

        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"RESET_PWD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    /** 错误验证码重置应失败且密码不变（SMS_CODE_INVALID = 1102） */
    @Test
    void resetWithWrongCodeShouldFail() throws Exception {
        String phone = randomPhone();
        registerUser(phone);

        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"RESET_PWD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"111111\",\"newPassword\":\"xyz67890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1102));

        // 密码未变更
        UserAuth auth = userAuthRepository.findByAuthTypeAndIdentifier(AuthType.PHONE, phone).orElseThrow();
        assertTrue(passwordEncoder.matches("abc12345", auth.getCredential()));
    }
}
