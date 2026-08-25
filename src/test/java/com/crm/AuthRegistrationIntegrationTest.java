package com.crm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 手机号验证码注册（UC-001 / 设计文档 4.4.1）集成测试。
 *
 * <p>依赖本地 PostgreSQL（docker compose 启动）：
 * 发送验证码走 Mock 短信通道（固定 000000），注册成功后返回 JWT Token。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 随机生成一个未注册手机号（避免测试数据冲突） */
    private String randomPhone() {
        return "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private void sendRegisterCode(String phone) throws Exception {
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 完整注册流程：发送验证码 → 000000 注册 → 返回 JWT Token */
    @Test
    void phoneRegisterFlowShouldSucceed() throws Exception {
        String phone = randomPhone();

        // 1. 发送注册验证码（Mock 通道）
        sendRegisterCode(phone);

        // 2. 使用固定验证码 000000 注册
        mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\","
                                + "\"password\":\"abc12345\",\"nickname\":\"测试用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.userNo").isNotEmpty())
                .andExpect(jsonPath("$.data.phoneMasked").value(phone.substring(0, 3) + "****" + phone.substring(7)))
                .andExpect(jsonPath("$.data.systemRole").value("NONE"));
    }

    /** 错误验证码应注册失败（SMS_CODE_INVALID = 1102） */
    @Test
    void registerWithWrongCodeShouldFail() throws Exception {
        String phone = randomPhone();
        sendRegisterCode(phone);

        mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"111111\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1102));
    }

    /** 同一手机号 60 秒内重复发送验证码应被限频（SMS_SEND_TOO_FREQUENT = 1101） */
    @Test
    void resendCodeWithin60sShouldBeRateLimited() throws Exception {
        String phone = randomPhone();
        sendRegisterCode(phone);

        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1101));
    }

    /** 重复注册同一手机号应提示已注册（PHONE_ALREADY_REGISTERED = 1001） */
    @Test
    void registerSamePhoneTwiceShouldFail() throws Exception {
        String phone = randomPhone();
        sendRegisterCode(phone);
        mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 再次发送验证码时，因手机号已注册被拦截
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
