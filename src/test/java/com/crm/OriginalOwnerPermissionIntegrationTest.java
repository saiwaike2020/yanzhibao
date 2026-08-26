package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.AccessLevel;
import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.GranteeType;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.OwnerType;
import com.crm.common.enums.PermissionLevel;
import com.crm.common.enums.ResourceType;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.resource.CreateResourceRequest;
import com.crm.dto.resource.ResourceResponse;
import com.crm.dto.resource.TransferOwnershipRequest;
import com.crm.entity.Company;
import com.crm.entity.CompanyMember;
import com.crm.entity.ResourcePermission;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.ResourcePermissionRepository;
import com.crm.repository.SysUserRepository;
import com.crm.service.ResourceOwnerService;
import com.crm.service.ResourcePermissionService;
import com.crm.service.ResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 原所有者权限调整（UC-034，v3.6）集成测试。
 *
 * <p>验证：个人用户将所有权分享（转让）给企业后，企业管理员可将原用户权限调整为
 * 无权（NONE）/ 只读（READ）/ 可写（WRITE）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OriginalOwnerPermissionIntegrationTest {

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
    private ResourcePermissionRepository resourcePermissionRepository;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceOwnerService resourceOwnerService;

    @Autowired
    private ResourcePermissionService resourcePermissionService;

    private String randomPhone() {
        return "132" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
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
        company.setCompanyNo("CPY_OO_" + System.nanoTime());
        company.setName(name);
        company.setOwnerUserId(ownerUserId);
        companyRepository.save(company);
        return company;
    }

    /** 直接创建企业成员记录 */
    private void createMember(Long companyId, Long userId, CompanyMemberRole role) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(MemberStatus.ACTIVE);
        companyMemberRepository.save(member);
    }

    /** 创建个人资源（归属创建者本人） */
    private Long createPersonalFile(Long ownerUserId) {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setResourceType(ResourceType.FILE);
        request.setName("分享测试文件");
        request.setOwnerType(OwnerType.USER);
        request.setOwnerId(ownerUserId);
        request.setFileSize(50L);
        ResourceResponse resource = resourceService.createResource(ownerUserId, request);
        return resource.getResourceId();
    }

    /** 原所有者将所有权转让给企业 */
    private void transferToCompany(Long resourceId, Long originalOwnerId, Long companyId) {
        TransferOwnershipRequest request = new TransferOwnershipRequest();
        request.setTargetOwnerType(OwnerType.COMPANY);
        request.setTargetOwnerId(companyId);
        request.setValidFrom(LocalDateTime.now());
        request.setValidUntil(null);
        resourceOwnerService.transferOwnership(resourceId, originalOwnerId, request);
    }

    private ResourcePermission permissionOf(Long resourceId, Long userId) {
        return resourcePermissionRepository
                .findByResourceIdAndGranteeTypeAndGranteeId(resourceId, GranteeType.USER, userId)
                .orElseThrow();
    }

    /** UC-034：转让给企业后，企业管理员可把原所有者设为 可写→只读→无权 */
    @Test
    void setOriginalOwnerReadWriteAndNone() throws Exception {
        Long originalOwnerId = registerAndGetUserId(randomPhone());
        Long adminId = registerAndGetUserId(randomPhone());
        Company company = createCompany(adminId, "原所有者权限测试企业A");
        createMember(company.getCompanyId(), adminId, CompanyMemberRole.OWNER);

        // 原所有者创建个人资源并转让给企业
        Long resourceId = createPersonalFile(originalOwnerId);
        transferToCompany(resourceId, originalOwnerId, company.getCompanyId());

        // 可写 WRITE
        resourcePermissionService.setOriginalOwnerPermission(
                resourceId, adminId, originalOwnerId, AccessLevel.WRITE);
        assertEquals(PermissionLevel.WRITE, permissionOf(resourceId, originalOwnerId).getPermissionLevel());

        // 调整为只读 READ（upsert 更新）
        resourcePermissionService.setOriginalOwnerPermission(
                resourceId, adminId, originalOwnerId, AccessLevel.READ);
        assertEquals(PermissionLevel.READ, permissionOf(resourceId, originalOwnerId).getPermissionLevel());

        // 调整为无权 NONE → 撤销全部授权
        resourcePermissionService.setOriginalOwnerPermission(
                resourceId, adminId, originalOwnerId, AccessLevel.NONE);
        assertFalse(resourcePermissionRepository
                .findByResourceIdAndGranteeTypeAndGranteeId(resourceId, GranteeType.USER, originalOwnerId)
                .isPresent());
    }

    /** UC-034：目标用户不是原所有者 → ORIGINAL_OWNER_NOT_FOUND（1412） */
    @Test
    void nonOriginalOwnerShouldFail() throws Exception {
        Long originalOwnerId = registerAndGetUserId(randomPhone());
        Long adminId = registerAndGetUserId(randomPhone());
        Long strangerId = registerAndGetUserId(randomPhone());
        Company company = createCompany(adminId, "原所有者权限测试企业B");
        createMember(company.getCompanyId(), adminId, CompanyMemberRole.OWNER);

        Long resourceId = createPersonalFile(originalOwnerId);
        transferToCompany(resourceId, originalOwnerId, company.getCompanyId());

        BusinessException e = assertThrows(BusinessException.class,
                () -> resourcePermissionService.setOriginalOwnerPermission(
                        resourceId, adminId, strangerId, AccessLevel.READ));
        assertEquals(ErrorCode.ORIGINAL_OWNER_NOT_FOUND.getCode(), e.getCode());
    }

    /** UC-034：操作者不是资源有效所有者 → NOT_RESOURCE_OWNER（1407） */
    @Test
    void nonOwnerOperatorShouldFail() throws Exception {
        Long originalOwnerId = registerAndGetUserId(randomPhone());
        Long adminId = registerAndGetUserId(randomPhone());
        Long outsiderId = registerAndGetUserId(randomPhone());
        Company company = createCompany(adminId, "原所有者权限测试企业C");
        createMember(company.getCompanyId(), adminId, CompanyMemberRole.OWNER);

        Long resourceId = createPersonalFile(originalOwnerId);
        transferToCompany(resourceId, originalOwnerId, company.getCompanyId());

        // 非企业成员尝试调整 → 无权限
        BusinessException e = assertThrows(BusinessException.class,
                () -> resourcePermissionService.setOriginalOwnerPermission(
                        resourceId, outsiderId, originalOwnerId, AccessLevel.READ));
        assertEquals(ErrorCode.NOT_RESOURCE_OWNER.getCode(), e.getCode());
    }
}
