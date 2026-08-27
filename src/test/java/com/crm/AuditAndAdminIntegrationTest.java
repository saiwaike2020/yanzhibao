package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.AuditScope;
import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
import com.crm.dto.audit.AuditLogQueryRequest;
import com.crm.dto.audit.AuditLogResponse;
import com.crm.dto.common.PageResponse;
import com.crm.dto.admin.AssignAuditorRequest;
import com.crm.dto.admin.UpdateAuditorRequest;
import com.crm.entity.AuditLog;
import com.crm.entity.AuditPermission;
import com.crm.entity.SysUser;
import com.crm.repository.AuditLogRepository;
import com.crm.repository.AuditPermissionRepository;
import com.crm.repository.SmsVerificationRepository;
import com.crm.repository.SysUserRepository;
import com.crm.security.LoginUser;
import com.crm.service.AuditService;
import com.crm.service.SystemAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 系统管理员与审计（用户管理 / 审计人员分配 / 审计范围判断）集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditAndAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private AuditPermissionRepository auditPermissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private AuditService auditService;

    private String randomPhone() {
        return "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /** 注册用户并返回 userId */
    private Long registerAndGetUserId(String phone) throws Exception {
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
        return sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
    }

    /** 模拟当前登录用户（系统管理员） */
    private void loginAs(Long userId, SystemRole role) {
        SysUser u = sysUserRepository.findById(userId).orElseThrow();
        LoginUser loginUser = new LoginUser(u.getUserId(), u.getUserNo(), u.getPhone(), role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    /** 直接插入 AUDITOR_ASSIGN 验证码记录（绕过 60 秒限频，Mock 码 000000） */
    private void sendAuditorAssignSms(String phone) {
        com.crm.entity.SmsVerification v = new com.crm.entity.SmsVerification();
        v.setPhone(phone);
        v.setScene(com.crm.common.enums.SmsScene.AUDITOR_ASSIGN);
        v.setCodeHash(sha256("000000"));
        v.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));
        v.setAttempts(0);
        v.setCreatedAt(java.time.LocalDateTime.now());
        smsVerificationRepository.save(v);
    }

    private String sha256(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 系统管理员用户管理：禁用 / 恢复 / 注销 */
    @Test
    void userManagementFlow() throws Exception {
        Long userId = registerAndGetUserId(randomPhone());

        systemAdminService.disableUser(userId);
        assertEquals(UserStatus.DISABLED, sysUserRepository.findById(userId).orElseThrow().getStatus());

        systemAdminService.restoreUser(userId);
        assertEquals(UserStatus.ACTIVE, sysUserRepository.findById(userId).orElseThrow().getStatus());

        systemAdminService.cancelUser(userId);
        assertEquals(UserStatus.CANCELLED, sysUserRepository.findById(userId).orElseThrow().getStatus());
    }

    /** UC-019：分配 / 调整 / 撤销审计人员权限（短信验证） */
    @Test
    void auditorAssignUpdateRevoke() throws Exception {
        Long adminId = registerAndGetUserId(randomPhone());
        Long auditorId = registerAndGetUserId(randomPhone());
        String adminPhone = sysUserRepository.findById(adminId).orElseThrow().getPhone();

        // 分配审计权限（短信发送会走 MockMvc 过滤器链，故发送后再模拟登录）
        sendAuditorAssignSms(adminPhone);
        loginAs(adminId, SystemRole.SYSTEM_ADMIN);
        AssignAuditorRequest assign = new AssignAuditorRequest();
        assign.setUserId(auditorId);
        assign.setAuditScope(AuditScope.ALL);
        assign.setSmsCode("000000");
        systemAdminService.assignAuditor(assign);

        assertEquals(SystemRole.AUDITOR, sysUserRepository.findById(auditorId).orElseThrow().getSystemRole());
        assertTrue(systemAdminService.listAuditors().stream().anyMatch(a -> a.getUserId().equals(auditorId)));

        // 调整审计范围
        sendAuditorAssignSms(adminPhone);
        loginAs(adminId, SystemRole.SYSTEM_ADMIN);
        UpdateAuditorRequest update = new UpdateAuditorRequest();
        update.setAuditScope(AuditScope.REGULAR_USERS);
        update.setSmsCode("000000");
        systemAdminService.updateAuditor(auditorId, update);
        assertEquals(AuditScope.REGULAR_USERS, auditPermissionRepository.findByUserId(auditorId).orElseThrow().getAuditScope());

        // 撤销审计角色（不依赖短信/登录）
        systemAdminService.revokeAuditor(auditorId);
        assertEquals(SystemRole.NONE, sysUserRepository.findById(auditorId).orElseThrow().getSystemRole());
        assertTrue(auditPermissionRepository.findByUserId(auditorId).isEmpty());
    }

    /** UC-020：审计范围（audit_scope + scope_details）过滤判断 */
    @Test
    void auditScopeFiltering() throws Exception {
        String userAction = "TEST_SCOPE_USER_" + System.nanoTime();
        String company1Action = "TEST_SCOPE_CO1_" + System.nanoTime();
        String company2Action = "TEST_SCOPE_CO2_" + System.nanoTime();
        String systemAction = "TEST_SCOPE_SYS_" + System.nanoTime();

        createLog("USER", null, userAction);
        createLog("COMPANY_USER", 100L, company1Action);
        createLog("COMPANY_USER", 200L, company2Action);
        createLog("SYSTEM", null, systemAction);

        java.util.Set<String> myActions = java.util.Set.of(userAction, company1Action, company2Action, systemAction);

        // 审计人员 A：ALL → 能看到 USER + 2 个 COMPANY_USER（3 条），看不到 SYSTEM
        Long auditorA = registerAndGetUserId(randomPhone());
        createPermission(auditorA, AuditScope.ALL, null);
        List<AuditLogResponse> logsA = auditService.queryAuditLogs(auditorA, new AuditLogQueryRequest()).getItems().stream()
                .filter(l -> myActions.contains(l.getAction())).toList();
        assertEquals(3, logsA.size());
        assertTrue(logsA.stream().noneMatch(l -> l.getAction().equals(systemAction)));

        // 审计人员 B：REGULAR_USERS → 只能看到 USER（1 条）
        Long auditorB = registerAndGetUserId(randomPhone());
        createPermission(auditorB, AuditScope.REGULAR_USERS, null);
        List<AuditLogResponse> logsB = auditService.queryAuditLogs(auditorB, new AuditLogQueryRequest()).getItems().stream()
                .filter(l -> myActions.contains(l.getAction())).toList();
        assertEquals(1, logsB.size());
        assertEquals(userAction, logsB.get(0).getAction());

        // 审计人员 C：ENTERPRISE_USERS + allowed_company_ids=[100] → 只能看到 company_id=100（1 条）
        Long auditorC = registerAndGetUserId(randomPhone());
        createPermission(auditorC, AuditScope.ENTERPRISE_USERS, Map.of("allowed_company_ids", List.of(100)));
        List<AuditLogResponse> logsC = auditService.queryAuditLogs(auditorC, new AuditLogQueryRequest()).getItems().stream()
                .filter(l -> myActions.contains(l.getAction())).toList();
        assertEquals(1, logsC.size());
        assertEquals(company1Action, logsC.get(0).getAction());
    }

    private AuditLog createLog(String userType, Long companyId, String action) {
        AuditLog log = new AuditLog();
        log.setUserId(1L);
        log.setUserType(userType);
        log.setCompanyId(companyId);
        log.setAction(action);
        log.setStatus(1);
        return auditLogRepository.save(log);
    }

    private void createPermission(Long userId, AuditScope scope, Map<String, Object> details) {
        AuditPermission ap = new AuditPermission();
        ap.setUserId(userId);
        ap.setAuditScope(scope);
        ap.setScopeDetails(details);
        ap.setGrantedBy(1L);
        auditPermissionRepository.save(ap);
    }
}
