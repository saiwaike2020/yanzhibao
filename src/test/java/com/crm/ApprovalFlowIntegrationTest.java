package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.ApprovalStatus;
import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.CompanyStatus;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.SystemRole;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.company.TransferCompanyRequest;
import com.crm.entity.Company;
import com.crm.entity.CompanyMember;
import com.crm.entity.SysUser;
import com.crm.repository.CompanyApprovalRepository;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.SysUserRepository;
import com.crm.service.CompanyMemberService;
import com.crm.service.CompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 审批流程（UC-032 加入申请审批 / UC-033 企业注销与所有权转让审批）集成测试。
 *
 * <p>验证：申请加入需企业所有者/管理员批准；企业注销与所有权转让需系统管理员或有权限的
 * 审计人员批准后才生效。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApprovalFlowIntegrationTest {

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
    private CompanyApprovalRepository companyApprovalRepository;

    @Autowired
    private CompanyMemberService companyMemberService;

    @Autowired
    private CompanyService companyService;

    private String randomPhone() {
        return "133" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
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

    /** 直接创建企业（CompanyService.createCompany 尚未实现，测试内直接写库） */
    private Company createCompany(Long ownerUserId, String name) {
        Company company = new Company();
        company.setCompanyNo("CPY_APR_" + System.nanoTime());
        company.setName(name);
        company.setOwnerUserId(ownerUserId);
        companyRepository.save(company);
        return company;
    }

    /** 直接创建企业成员记录 */
    private void createMember(Long companyId, Long userId, CompanyMemberRole role, MemberStatus status) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(status);
        companyMemberRepository.save(member);
    }

    /** 将用户设置为系统管理员 */
    private void makeSystemAdmin(Long userId) {
        SysUser user = sysUserRepository.findById(userId).orElseThrow();
        user.setSystemRole(SystemRole.SYSTEM_ADMIN);
        sysUserRepository.save(user);
    }

    private CompanyMember memberOf(Long companyId, Long userId) {
        return companyMemberRepository.findByCompanyIdAndUserId(companyId, userId).orElseThrow();
    }

    /** UC-032：申请加入 → 企业所有者批准 → ACTIVE；拒绝 → EXITED */
    @Test
    void joinRequestApproveAndRejectFlow() throws Exception {
        Long ownerId = registerAndGetUserId(randomPhone());
        Long applicantId = registerAndGetUserId(randomPhone());
        Company company = createCompany(ownerId, "审批测试企业A");
        createMember(company.getCompanyId(), ownerId, CompanyMemberRole.OWNER, MemberStatus.ACTIVE);

        // 申请加入 → INVITED
        companyMemberService.applyJoinCompany(company.getCompanyId(), applicantId);
        assertEquals(MemberStatus.INVITED, memberOf(company.getCompanyId(), applicantId).getStatus());

        // 批准 → ACTIVE
        Long memberId = memberOf(company.getCompanyId(), applicantId).getMemberId();
        companyMemberService.approveJoinRequest(company.getCompanyId(), ownerId, memberId);
        assertEquals(MemberStatus.ACTIVE, memberOf(company.getCompanyId(), applicantId).getStatus());

        // 再注册一人申请，拒绝 → EXITED
        Long applicant2Id = registerAndGetUserId(randomPhone());
        companyMemberService.applyJoinCompany(company.getCompanyId(), applicant2Id);
        Long member2Id = memberOf(company.getCompanyId(), applicant2Id).getMemberId();
        companyMemberService.rejectJoinRequest(company.getCompanyId(), ownerId, member2Id);
        assertEquals(MemberStatus.EXITED, memberOf(company.getCompanyId(), applicant2Id).getStatus());
    }

    /** UC-033：企业注销须系统管理员批准后生效，普通用户无审批权限 */
    @Test
    void dissolveCompanyRequiresSystemAdminApproval() throws Exception {
        Long ownerId = registerAndGetUserId(randomPhone());
        Long normalUserId = registerAndGetUserId(randomPhone());
        Company company = createCompany(ownerId, "注销审批测试企业B");
        createMember(company.getCompanyId(), ownerId, CompanyMemberRole.OWNER, MemberStatus.ACTIVE);

        // 所有者发起注销申请 → PENDING
        companyService.applyDissolveCompany(company.getCompanyId(), ownerId);
        assertEquals(ApprovalStatus.PENDING, companyApprovalRepository
                .findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING).get(0).getStatus());

        // 普通用户审批 → 无权限
        BusinessException e = assertThrows(BusinessException.class,
                () -> companyService.reviewCompanyApproval(
                        companyApprovalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING).get(0).getApprovalId(),
                        normalUserId, true, "try"));
        assertEquals(ErrorCode.NO_APPROVAL_PERMISSION.getCode(), e.getCode());

        // 系统管理员批准 → 企业 DISSOLVED
        makeSystemAdmin(normalUserId);
        companyService.reviewCompanyApproval(
                companyApprovalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING).get(0).getApprovalId(),
                normalUserId, true, "同意注销");
        assertEquals(CompanyStatus.DISSOLVED, companyRepository.findById(company.getCompanyId())
                .orElseThrow().getStatus());
        assertEquals(ApprovalStatus.APPROVED, companyApprovalRepository
                .findByStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED).get(0).getStatus());
    }

    /** UC-033：企业所有权转让须系统管理员批准后生效 */
    @Test
    void transferOwnershipRequiresApproval() throws Exception {
        Long ownerId = registerAndGetUserId(randomPhone());
        Long targetId = registerAndGetUserId(randomPhone());
        Long adminId = registerAndGetUserId(randomPhone());
        makeSystemAdmin(adminId);

        Company company = createCompany(ownerId, "转让审批测试企业C");
        createMember(company.getCompanyId(), ownerId, CompanyMemberRole.OWNER, MemberStatus.ACTIVE);
        createMember(company.getCompanyId(), targetId, CompanyMemberRole.MEMBER, MemberStatus.ACTIVE);

        // 所有者发起转让申请
        TransferCompanyRequest request = new TransferCompanyRequest();
        request.setNewOwnerUserId(targetId);
        companyService.applyTransferOwnership(company.getCompanyId(), ownerId, request);

        // 系统管理员批准
        Long approvalId = companyApprovalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING)
                .get(0).getApprovalId();
        companyService.reviewCompanyApproval(approvalId, adminId, true, "同意转让");

        // 企业所有者更新为目标用户；角色调整：原所有者 → MEMBER，目标 → OWNER
        Company updated = companyRepository.findById(company.getCompanyId()).orElseThrow();
        assertEquals(targetId, updated.getOwnerUserId());
        assertEquals(CompanyMemberRole.MEMBER, memberOf(company.getCompanyId(), ownerId).getRole());
        assertEquals(CompanyMemberRole.OWNER, memberOf(company.getCompanyId(), targetId).getRole());
    }
}
