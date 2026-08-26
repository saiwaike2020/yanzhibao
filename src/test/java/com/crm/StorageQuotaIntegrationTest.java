package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.dto.admin.SystemSettingRequest;
import com.crm.repository.SysUserRepository;
import com.crm.service.ResourceService;
import com.crm.service.SystemAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 存储配额（UC-029 创建文件配额校验 / UC-030 系统管理员调节配额）集成测试。
 *
 * <p>验证：资源存储设有上限，按归属主体（个人 / 企业）核算，系统管理员可动态调节配额。
 */
@SpringBootTest
@AutoConfigureMockMvc
class StorageQuotaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private ResourceService resourceService;

    private String randomPhone() {
        return "135" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /** 注册用户并返回 JWT Token */
    private String registerAndGetToken(String phone) throws Exception {
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        String resp = mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).path("data").path("token").asText();
    }

    /** 注册用户并返回 userId */
    private Long registerAndGetUserId(String phone) throws Exception {
        registerAndGetToken(phone);
        return sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
    }

    /** 通过系统管理员接口调整个人存储配额（字节） */
    private void setPersonalQuota(long bytes) {
        SystemSettingRequest request = new SystemSettingRequest();
        request.setKey(ResourceService.KEY_QUOTA_PERSONAL);
        request.setValue(String.valueOf(bytes));
        systemAdminService.updateSystemSetting(request);
    }

    /** 创建文件资源，返回 MockMvc 结果供断言 */
    private MvcResult createFile(String token, Long ownerUserId, long fileSize) throws Exception {
        return mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"FILE\",\"name\":\"配额测试文件\","
                                + "\"ownerType\":\"USER\",\"ownerId\":" + ownerUserId + ","
                                + "\"fileSize\":" + fileSize + ",\"fileType\":\"text/plain\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** UC-029：单文件超过个人配额 → 拒绝；配额内 → 成功 */
    @Test
    void createFileExceedingPersonalQuotaShouldFail() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        Long userId = sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
        setPersonalQuota(1000);

        // 2000 字节 > 1000 字节配额 → 拒绝
        String reject = createFile(token, userId, 2000).getResponse().getContentAsString();
        assertEquals(1602, objectMapper.readTree(reject).path("code").asInt());

        // 500 字节 <= 1000 字节配额 → 成功
        String ok = createFile(token, userId, 500).getResponse().getContentAsString();
        assertEquals(0, objectMapper.readTree(ok).path("code").asInt());
    }

    /** UC-029：存储占用实时累加，超出配额后禁止继续上传 */
    @Test
    void storageUsageAccumulatesWithinQuota() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        Long userId = sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
        setPersonalQuota(1000);

        // 600 字节 → 成功（已用 600）
        assertEquals(0, objectMapper.readTree(
                createFile(token, userId, 600).getResponse().getContentAsString()).path("code").asInt());

        // 再传 500 字节 → 600 + 500 = 1100 > 1000 → 拒绝
        assertEquals(1602, objectMapper.readTree(
                createFile(token, userId, 500).getResponse().getContentAsString()).path("code").asInt());

        // 再传 400 字节 → 600 + 400 = 1000 <= 1000 → 成功
        assertEquals(0, objectMapper.readTree(
                createFile(token, userId, 400).getResponse().getContentAsString()).path("code").asInt());
    }

    /** UC-030：系统管理员调节存储配额，非法值被拒绝 */
    @Test
    void adminCanAdjustStorageQuota() throws Exception {
        // 调高个人配额
        SystemSettingRequest request = new SystemSettingRequest();
        request.setKey(ResourceService.KEY_QUOTA_PERSONAL);
        request.setValue("2000");
        systemAdminService.updateSystemSetting(request);

        // 查询确认生效
        assertEquals("2000", systemAdminService
                .getSystemSetting(ResourceService.KEY_QUOTA_PERSONAL).getValue());

        // 非数字
        request.setValue("abc");
        org.junit.jupiter.api.Assertions.assertThrows(com.crm.common.exception.BusinessException.class,
                () -> systemAdminService.updateSystemSetting(request));

        // 0
        request.setValue("0");
        org.junit.jupiter.api.Assertions.assertThrows(com.crm.common.exception.BusinessException.class,
                () -> systemAdminService.updateSystemSetting(request));

        // 负数
        request.setValue("-5");
        org.junit.jupiter.api.Assertions.assertThrows(com.crm.common.exception.BusinessException.class,
                () -> systemAdminService.updateSystemSetting(request));

        // 非法值未生效，配额保持 2000
        assertEquals("2000", systemAdminService
                .getSystemSetting(ResourceService.KEY_QUOTA_PERSONAL).getValue());
    }
}
