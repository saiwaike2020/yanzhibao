package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.MemberStatus;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.auth.PhonePasswordLoginRequest;
import com.crm.dto.auth.SetPasswordRequest;
import com.crm.dto.company.CompanyResponse;
import com.crm.dto.company.CreateCompanyRequest;
import com.crm.dto.company.InviteMemberRequest;
import com.crm.dto.user.UpdateProfileRequest;
import com.crm.entity.CompanyMember;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.SysUserRepository;
import com.crm.service.AuthService;
import com.crm.service.CompanyMemberService;
import com.crm.service.CompanyService;
import com.crm.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 核心功能（登录 / 密码设置 / 创建企业 / 接受邀请 / 个人中心 / 成员管理）集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoreFeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository companyMemberRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyMemberService companyMemberService;

    @Autowired
    private UserService userService;

    private String randomPhone() {
        return "130" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /** 注册用户（密码 abc12345）并返回 JWT Token */
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

    /** 手机号密码登录 */
    private void login(String phone, String password, int expectCode) {
        PhonePasswordLoginRequest request = new PhonePasswordLoginRequest();
        request.setPhone(phone);
        request.setPassword(password);
        if (expectCode == 0) {
            assertNotNull(authService.loginByPhonePassword(request).getToken());
        } else {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> authService.loginByPhonePassword(request));
            assertEquals(expectCode, e.getCode());
        }
    }

    /** UC-003：手机号密码登录成功 / 密码错误 */
    @Test
    void loginByPhonePasswordSucceedAndFail() throws Exception {
        String phone = randomPhone();
        registerAndGetToken(phone);
        login(phone, "abc12345", 0);
        login(phone, "wrong-pass", ErrorCode.LOGIN_FAILED.getCode());
    }

    /** 设置 / 修改登录密码 */
    @Test
    void setAndChangePassword() throws Exception {
        String phone = randomPhone();
        Long userId = registerAndGetUserId(phone);

        // 修改密码（提供旧密码）
        SetPasswordRequest change = new SetPasswordRequest();
        change.setNewPassword("xyz67890");
        change.setOldPassword("abc12345");
        authService.setPassword(userId, change);

        // 新密码可登录，旧密码失败
        login(phone, "xyz67890", 0);
        login(phone, "abc12345", ErrorCode.LOGIN_FAILED.getCode());
    }

    /** UC-005：创建企业，创建者成为 OWNER */
    @Test
    void createCompanyShouldSucceed() throws Exception {
        Long userId = registerAndGetUserId(randomPhone());
        CreateCompanyRequest request = new CreateCompanyRequest();
        request.setName("核心功能测试企业");
        CompanyResponse company = companyService.createCompany(userId, request);

        assertNotNull(company.getCompanyId());
        assertEquals("核心功能测试企业", company.getName());
        assertEquals(userId, company.getOwnerUserId());

        CompanyMember owner = companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), userId).orElseThrow();
        assertEquals(CompanyMemberRole.OWNER, owner.getRole());
        assertEquals(MemberStatus.ACTIVE, owner.getStatus());
    }

    /** UC-037：企业邀请 → 被邀请用户接受邀请 → ACTIVE */
    @Test
    void inviteAndAcceptInvitation() throws Exception {
        Long adminId = registerAndGetUserId(randomPhone());
        String targetPhone = randomPhone();
        Long targetId = registerAndGetUserId(targetPhone);

        CompanyResponse company = companyService.createCompany(adminId,
                new CreateCompanyRequest() {{ setName("邀请测试企业"); }});

        InviteMemberRequest invite = new InviteMemberRequest();
        invite.setPhone(targetPhone);
        companyMemberService.inviteMember(company.getCompanyId(), adminId, invite);

        // 被邀请用户接受邀请
        companyMemberService.acceptInvitation(company.getCompanyId(), targetId);
        CompanyMember member = companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), targetId).orElseThrow();
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
    }

    /** 个人中心：查看资料 / 更新资料 / 账号安全 */
    @Test
    void userProfileAndSecurity() throws Exception {
        Long userId = registerAndGetUserId(randomPhone());

        // 查看当前用户
        assertNotNull(userService.getCurrentUser(userId));

        // 更新资料
        UpdateProfileRequest update = new UpdateProfileRequest();
        update.setNickname("新昵称");
        userService.updateProfile(userId, update);
        assertEquals("新昵称", userService.getCurrentUser(userId).getNickname());

        // 账号安全：已绑定手机号、已设置密码
        var security = userService.getAccountSecurity(userId);
        assertNotNull(security.getPhoneMasked());
        assertTrue(security.isHasPassword());
    }

    /** 成员管理：禁用 / 恢复 / 移除 / 退出 */
    @Test
    void memberManagementFlow() throws Exception {
        Long adminId = registerAndGetUserId(randomPhone());
        Long memberId = registerAndGetUserId(randomPhone());
        CompanyResponse company = companyService.createCompany(adminId,
                new CreateCompanyRequest() {{ setName("成员管理测试企业"); }});

        // 邀请成员并接受
        InviteMemberRequest invite = new InviteMemberRequest();
        invite.setPhone(sysUserRepository.findById(memberId).orElseThrow().getPhone());
        companyMemberService.inviteMember(company.getCompanyId(), adminId, invite);
        companyMemberService.acceptInvitation(company.getCompanyId(), memberId);

        CompanyMember member = companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), memberId).orElseThrow();

        // 禁用 → DISABLED
        companyMemberService.disableMember(company.getCompanyId(), member.getMemberId());
        assertEquals(MemberStatus.DISABLED, companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), memberId).orElseThrow().getStatus());

        // 恢复 → ACTIVE
        companyMemberService.restoreMember(company.getCompanyId(), member.getMemberId());
        assertEquals(MemberStatus.ACTIVE, companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), memberId).orElseThrow().getStatus());

        // 移除 → EXITED（逻辑失效）
        companyMemberService.removeMember(company.getCompanyId(), member.getMemberId());
        assertEquals(MemberStatus.EXITED, companyMemberRepository
                .findByCompanyIdAndUserId(company.getCompanyId(), memberId).orElseThrow().getStatus());
    }
}
