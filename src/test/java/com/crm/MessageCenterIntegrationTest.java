package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.MessageType;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.dto.company.InviteMemberRequest;
import com.crm.dto.message.UserMessageResponse;
import com.crm.entity.Company;
import com.crm.entity.CompanyMember;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.SysUserRepository;
import com.crm.service.CompanyMemberService;
import com.crm.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 消息中心（UC-026 申请加入 / UC-027 邀请加入 / UC-028 消息中心）集成测试。
 *
 * <p>依赖本地 PostgreSQL（docker compose 启动）。企业创建逻辑（CompanyService）尚未实现，
 * 测试内直接通过仓储构造企业与成员数据后，走服务层验证消息联动。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MessageCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CompanyMemberService companyMemberService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository companyMemberRepository;

    @Autowired
    private SysUserRepository sysUserRepository;

    private String randomPhone() {
        return "137" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
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

    /** 直接创建企业（CompanyService 尚未实现，测试内直接写库） */
    private Company createCompany(Long ownerUserId, String name) {
        Company company = new Company();
        company.setCompanyNo("CPY_TEST_" + System.nanoTime());
        company.setName(name);
        company.setOwnerUserId(ownerUserId);
        companyRepository.save(company);
        return company;
    }

    /** 直接创建企业成员记录（默认 ACTIVE） */
    private void createMember(Long companyId, Long userId, CompanyMemberRole role) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(MemberStatus.ACTIVE);
        companyMemberRepository.save(member);
    }

    /** UC-028 消息中心：分页查询 / 未读数 / 标记已读 / 全部已读 */
    @Test
    void messageCenterQueryAndReadFlowShouldWork() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        Long userId = sysUserRepository.findByPhone(phone).orElseThrow().getUserId();

        // 通过服务发送 3 条系统消息
        messageService.sendSystemMessage(userId, "系统通知", "欢迎使用CRM系统");
        messageService.sendSystemMessage(userId, "安全提醒", "请及时修改密码");
        messageService.sendSystemMessage(userId, "活动通知", "新版本上线");

        // 未读数 = 3
        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(3));

        // 分页查询：page=1 size=2 → total=3，返回 2 条
        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        // 标记第 1 条消息已读 → 未读数 = 2
        PageResponse<UserMessageResponse> page = messageService.listMessages(userId, new PageQueryRequest());
        Long firstMessageId = page.getItems().get(0).getMessageId();
        mockMvc.perform(put("/api/messages/{id}/read", firstMessageId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        // 全部标记已读 → 未读数 = 0
        mockMvc.perform(put("/api/messages/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    /** UC-027 邀请成员加入 → 被邀请用户收到 INVITATION 消息 */
    @Test
    void inviteMemberShouldNotifyTargetUser() throws Exception {
        Long adminId = registerAndGetUserId(randomPhone());
        String targetPhone = randomPhone();
        Long targetId = registerAndGetUserId(targetPhone);

        Company company = createCompany(adminId, "消息测试企业A");
        createMember(company.getCompanyId(), adminId, CompanyMemberRole.OWNER);

        InviteMemberRequest request = new InviteMemberRequest();
        request.setPhone(targetPhone);
        request.setNote("欢迎加入");
        companyMemberService.inviteMember(company.getCompanyId(), adminId, request);

        PageResponse<UserMessageResponse> msgs = messageService.listMessages(targetId, new PageQueryRequest());
        assertEquals(1, msgs.getTotal());
        assertEquals(MessageType.INVITATION, msgs.getItems().get(0).getMessageType());
        assertTrue(msgs.getItems().get(0).getContent().contains("消息测试企业A"));
    }

    /** UC-026 用户申请加入企业 → 企业管理员收到 JOIN_REQUEST 消息 */
    @Test
    void applyJoinCompanyShouldNotifyAdmins() throws Exception {
        Long adminId = registerAndGetUserId(randomPhone());
        Long applicantId = registerAndGetUserId(randomPhone());

        Company company = createCompany(adminId, "消息测试企业B");
        createMember(company.getCompanyId(), adminId, CompanyMemberRole.ADMIN);

        companyMemberService.applyJoinCompany(company.getCompanyId(), applicantId);

        PageResponse<UserMessageResponse> msgs = messageService.listMessages(adminId, new PageQueryRequest());
        assertEquals(1, msgs.getTotal());
        assertEquals(MessageType.JOIN_REQUEST, msgs.getItems().get(0).getMessageType());
        assertTrue(msgs.getItems().get(0).getContent().contains("消息测试企业B"));
    }
}
