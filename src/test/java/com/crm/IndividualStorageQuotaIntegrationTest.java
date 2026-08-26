package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.OwnerType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.admin.StorageQuotaRequest;
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

/**
 * 个体存储配额（UC-031：系统管理员针对单个用户 / 个别企业设置专属配额）集成测试。
 *
 * <p>验证：个体专属配额**优先于**全局默认配额；移除个体配额后恢复使用默认配额。
 */
@SpringBootTest
@AutoConfigureMockMvc
class IndividualStorageQuotaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private SystemAdminService systemAdminService;

    private String randomPhone() {
        return "134" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
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

    /** 设置全局默认个人配额（字节） */
    private void setPersonalQuota(long bytes) {
        SystemSettingRequest request = new SystemSettingRequest();
        request.setKey(ResourceService.KEY_QUOTA_PERSONAL);
        request.setValue(String.valueOf(bytes));
        systemAdminService.updateSystemSetting(request);
    }

    /** 设置个体配额（字节） */
    private void setIndividualQuota(Long userId, long bytes) {
        StorageQuotaRequest request = new StorageQuotaRequest();
        request.setQuotaType(OwnerType.USER);
        request.setSubjectId(userId);
        request.setQuotaBytes(bytes);
        systemAdminService.setStorageQuota(request, 1L);
    }

    /** 创建文件，返回响应 code */
    private int createFile(String token, Long ownerUserId, long fileSize) throws Exception {
        String resp = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"FILE\",\"name\":\"个体配额测试文件\","
                                + "\"ownerType\":\"USER\",\"ownerId\":" + ownerUserId + ","
                                + "\"fileSize\":" + fileSize + ",\"fileType\":\"text/plain\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).path("code").asInt();
    }

    /** UC-031：个体专属配额优先于全局默认配额 */
    @Test
    void individualQuotaOverridesDefault() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        Long userId = sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
        setPersonalQuota(1000);

        // 个体配额 500，覆盖全局默认 1000
        setIndividualQuota(userId, 500);

        // 600 > 500 → 拒绝
        assertEquals(1602, createFile(token, userId, 600));

        // 400 <= 500 → 成功
        assertEquals(0, createFile(token, userId, 400));

        // 查询个体配额确认
        assertEquals(500, systemAdminService
                .getStorageQuota(OwnerType.USER, userId).getQuotaBytes());
    }

    /** UC-031：移除个体配额后恢复使用全局默认配额 */
    @Test
    void removingIndividualQuotaRestoresDefault() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        Long userId = sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
        setPersonalQuota(1000);

        // 设个体 500，600 被拒
        setIndividualQuota(userId, 500);
        assertEquals(1602, createFile(token, userId, 600));

        // 移除个体配额
        systemAdminService.removeStorageQuota(OwnerType.USER, userId);

        // 恢复默认 1000，600 成功
        assertEquals(0, createFile(token, userId, 600));

        // 查询已不存在的个体配额 → 1604
        assertThrows(BusinessException.class,
                () -> systemAdminService.getStorageQuota(OwnerType.USER, userId));
    }

    /** UC-031：非法配额值与查询不存在的主体 */
    @Test
    void invalidQuotaValueAndMissingSubjectShouldFail() throws Exception {
        Long userId = registerAndGetUserId(randomPhone());

        // 0 → 1603
        StorageQuotaRequest zero = new StorageQuotaRequest();
        zero.setQuotaType(OwnerType.USER);
        zero.setSubjectId(userId);
        zero.setQuotaBytes(0L);
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> systemAdminService.setStorageQuota(zero, 1L));
        assertEquals(ErrorCode.INVALID_QUOTA_VALUE.getCode(), e1.getCode());

        // -1 → 1603
        zero.setQuotaBytes(-1L);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> systemAdminService.setStorageQuota(zero, 1L));
        assertEquals(ErrorCode.INVALID_QUOTA_VALUE.getCode(), e2.getCode());

        // 查询未设置的主体 → 1604
        BusinessException e3 = assertThrows(BusinessException.class,
                () -> systemAdminService.getStorageQuota(OwnerType.USER, userId));
        assertEquals(ErrorCode.STORAGE_QUOTA_NOT_FOUND.getCode(), e3.getCode());
    }
}
