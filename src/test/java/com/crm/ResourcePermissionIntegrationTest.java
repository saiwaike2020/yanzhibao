package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.repository.SysUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 资源权限有效期（UC-011 / UC-012 / 设计文档 6.4「授权有效期」）集成测试。
 *
 * <p>验证：资源的读、写、所有权授权通过起始可用日期（validFrom，必填）与过期时间
 * （validUntil，可选）控制有效期；未填起始日期 / 过期早于起始 / 重复授权均被拒绝。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResourcePermissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    private String randomPhone() {
        return "136" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
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

    /** 创建个人资源（当前用户为所有者），返回 resourceId */
    private Long createResource(String token, Long ownerUserId) throws Exception {
        String resp = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"LIBRARY\",\"name\":\"有效期测试资料库\","
                                + "\"ownerType\":\"USER\",\"ownerId\":" + ownerUserId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).path("data").path("resourceId").asLong();
    }

    /** 给用户授权（带有效期） */
    private void grantToUser(String token, Long resourceId, Long targetUserId, String level,
                             String validFrom, String validUntil) throws Exception {
        String body = "{\"granteeType\":\"USER\",\"granteeId\":" + targetUserId
                + ",\"permissionLevel\":\"" + level + "\"";
        if (validFrom != null) {
            body += ",\"validFrom\":\"" + validFrom + "\"";
        }
        if (validUntil != null) {
            body += ",\"validUntil\":\"" + validUntil + "\"";
        }
        body += "}";
        mockMvc.perform(post("/api/resources/{id}/permissions/users", resourceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    /** 授权（带有效期）成功，列表返回有效期字段 */
    @Test
    void grantPermissionWithValidityShouldSucceed() throws Exception {
        String ownerPhone = randomPhone();
        String ownerToken = registerAndGetToken(ownerPhone);
        Long ownerId = sysUserRepository.findByPhone(ownerPhone).orElseThrow().getUserId();
        Long targetId = registerAndGetUserId(randomPhone());
        Long resourceId = createResource(ownerToken, ownerId);

        grantToUser(ownerToken, resourceId, targetId, "READ", "2026-01-01T00:00:00", "2030-01-01T00:00:00");

        mockMvc.perform(get("/api/resources/{id}/permissions", resourceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].permissionLevel").value("READ"))
                .andExpect(jsonPath("$.data[0].validFrom").value("2026-01-01T00:00:00"))
                .andExpect(jsonPath("$.data[0].validUntil").value("2030-01-01T00:00:00"))
                .andExpect(jsonPath("$.data[0].granteeId").value(targetId));
    }

    /** 起始可用日期未填 → 参数校验失败（code=400） */
    @Test
    void grantWithoutValidFromShouldFail() throws Exception {
        String ownerPhone = randomPhone();
        String ownerToken = registerAndGetToken(ownerPhone);
        Long ownerId = sysUserRepository.findByPhone(ownerPhone).orElseThrow().getUserId();
        Long targetId = registerAndGetUserId(randomPhone());
        Long resourceId = createResource(ownerToken, ownerId);

        mockMvc.perform(post("/api/resources/{id}/permissions/users", resourceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granteeType\":\"USER\",\"granteeId\":" + targetId
                                + ",\"permissionLevel\":\"READ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("起始可用日期不能为空"));
    }

    /** 过期时间早于起始日期 → 拒绝（OWNERSHIP_VALIDITY_INVALID = 1409） */
    @Test
    void grantWithInvalidValidityShouldFail() throws Exception {
        String ownerPhone = randomPhone();
        String ownerToken = registerAndGetToken(ownerPhone);
        Long ownerId = sysUserRepository.findByPhone(ownerPhone).orElseThrow().getUserId();
        Long targetId = registerAndGetUserId(randomPhone());
        Long resourceId = createResource(ownerToken, ownerId);

        mockMvc.perform(post("/api/resources/{id}/permissions/users", resourceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granteeType\":\"USER\",\"granteeId\":" + targetId
                                + ",\"permissionLevel\":\"READ\","
                                + "\"validFrom\":\"2026-01-01T00:00:00\","
                                + "\"validUntil\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1409));
    }

    /** 同一授权主体重复授权 → PERMISSION_ALREADY_EXISTS = 1411 */
    @Test
    void duplicateGrantShouldFail() throws Exception {
        String ownerPhone = randomPhone();
        String ownerToken = registerAndGetToken(ownerPhone);
        Long ownerId = sysUserRepository.findByPhone(ownerPhone).orElseThrow().getUserId();
        Long targetId = registerAndGetUserId(randomPhone());
        Long resourceId = createResource(ownerToken, ownerId);

        grantToUser(ownerToken, resourceId, targetId, "READ", "2026-01-01T00:00:00", null);

        mockMvc.perform(post("/api/resources/{id}/permissions/users", resourceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granteeType\":\"USER\",\"granteeId\":" + targetId
                                + ",\"permissionLevel\":\"WRITE\","
                                + "\"validFrom\":\"2026-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1411));
    }

    /** 更新权限级别 / 有效期，然后撤销授权 */
    @Test
    void updateAndRevokePermissionShouldWork() throws Exception {
        String ownerPhone = randomPhone();
        String ownerToken = registerAndGetToken(ownerPhone);
        Long ownerId = sysUserRepository.findByPhone(ownerPhone).orElseThrow().getUserId();
        Long targetId = registerAndGetUserId(randomPhone());
        Long resourceId = createResource(ownerToken, ownerId);

        grantToUser(ownerToken, resourceId, targetId, "READ", "2026-01-01T00:00:00", null);

        // 查询授权记录 ID
        String listResp = mockMvc.perform(get("/api/resources/{id}/permissions", resourceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long permissionId = objectMapper.readTree(listResp).path("data").get(0).path("permissionId").asLong();

        // 更新为 WRITE + 新有效期
        mockMvc.perform(put("/api/resources/{rid}/permissions/{pid}", resourceId, permissionId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionLevel\":\"WRITE\","
                                + "\"validFrom\":\"2026-02-01T00:00:00\","
                                + "\"validUntil\":\"2031-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 校验已更新
        mockMvc.perform(get("/api/resources/{id}/permissions", resourceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].permissionLevel").value("WRITE"))
                .andExpect(jsonPath("$.data[0].validUntil").value("2031-01-01T00:00:00"));

        // 撤销授权
        mockMvc.perform(delete("/api/resources/{rid}/permissions/{pid}", resourceId, permissionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 列表为空
        String emptyResp = mockMvc.perform(get("/api/resources/{id}/permissions", resourceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, objectMapper.readTree(emptyResp).path("data").size());
    }
}
