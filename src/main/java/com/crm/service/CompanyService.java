package com.crm.service;

import com.crm.common.enums.ApprovalStatus;
import com.crm.common.enums.ApprovalType;
import com.crm.common.enums.AuditScope;
import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.CompanyStatus;
import com.crm.common.enums.MemberStatus;
import com.crm.common.enums.SystemRole;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.company.CompanyResponse;
import com.crm.dto.company.CreateCompanyRequest;
import com.crm.dto.company.TransferCompanyRequest;
import com.crm.dto.company.UpdateCompanyRequest;
import com.crm.entity.AuditPermission;
import com.crm.entity.Company;
import com.crm.entity.CompanyApproval;
import com.crm.entity.CompanyMember;
import com.crm.entity.SysUser;
import com.crm.repository.AuditPermissionRepository;
import com.crm.repository.CompanyApprovalRepository;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.SysUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业服务（创建企业、企业信息、所有权转让、注销）。
 *
 * <p>v3.5：企业注销（DISSOLVE）与所有权转让（TRANSFER）由企业所有者发起申请，
 * 经**系统管理员或有权限的审计人员**批准后方可生效（UC-033，company_approvals 审批记录）。
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyApprovalRepository companyApprovalRepository;
    private final SysUserRepository sysUserRepository;
    private final AuditPermissionRepository auditPermissionRepository;

    /** 创建企业，创建者默认成为 OWNER (UC-005) */
    public CompanyResponse createCompany(Long userId, CreateCompanyRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 当前用户加入的企业列表 */
    public List<CompanyResponse> listMyCompanies(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 企业详情 */
    public CompanyResponse getCompanyById(Long companyId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 更新企业信息（仅企业管理员） */
    public CompanyResponse updateCompany(Long companyId, UpdateCompanyRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 发起企业注销申请（UC-018 / UC-033）：企业所有者操作，创建 DISSOLVE 待审批记录 */
    @Transactional
    public void applyDissolveCompany(Long companyId, Long operatorUserId) {
        Company company = findCompany(companyId);
        ensureCompanyOwner(company, operatorUserId);
        ensureNoPendingApproval(companyId);

        CompanyApproval approval = new CompanyApproval();
        approval.setCompanyId(companyId);
        approval.setApprovalType(ApprovalType.DISSOLVE);
        approval.setRequesterUserId(operatorUserId);
        approval.setStatus(ApprovalStatus.PENDING);
        companyApprovalRepository.save(approval);
    }

    /** 发起企业所有权转让申请（UC-033）：企业所有者操作，创建 TRANSFER 待审批记录 */
    @Transactional
    public void applyTransferOwnership(Long companyId, Long operatorUserId, TransferCompanyRequest request) {
        Company company = findCompany(companyId);
        ensureCompanyOwner(company, operatorUserId);
        ensureNoPendingApproval(companyId);

        // 目标用户须为企业活跃成员
        CompanyMember target = companyMemberRepository
                .findByCompanyIdAndUserId(companyId, request.getNewOwnerUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (target.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        CompanyApproval approval = new CompanyApproval();
        approval.setCompanyId(companyId);
        approval.setApprovalType(ApprovalType.TRANSFER);
        approval.setRequesterUserId(operatorUserId);
        approval.setTargetUserId(request.getNewOwnerUserId());
        approval.setStatus(ApprovalStatus.PENDING);
        companyApprovalRepository.save(approval);
    }

    /** 审批企业变更申请（UC-033）：系统管理员或有权限的审计人员 */
    @Transactional
    public void reviewCompanyApproval(Long approvalId, Long reviewerUserId, boolean approved, String note) {
        CompanyApproval approval = companyApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_HANDLED);
        }
        if (!isApproverAuthorized(reviewerUserId, approval.getCompanyId())) {
            throw new BusinessException(ErrorCode.NO_APPROVAL_PERMISSION);
        }

        approval.setReviewedBy(reviewerUserId);
        approval.setReviewNote(note);
        approval.setReviewedAt(LocalDateTime.now());
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        companyApprovalRepository.save(approval);

        if (!approved) {
            return;
        }

        // 批准后执行变更
        Company company = findCompany(approval.getCompanyId());
        if (approval.getApprovalType() == ApprovalType.DISSOLVE) {
            // 注销：企业状态置为 DISSOLVED，成员标记 EXITED
            company.setStatus(CompanyStatus.DISSOLVED);
            companyRepository.save(company);
            companyMemberRepository.findByCompanyId(company.getCompanyId()).forEach(m -> {
                m.setStatus(MemberStatus.EXITED);
                companyMemberRepository.save(m);
            });
        } else if (approval.getApprovalType() == ApprovalType.TRANSFER) {
            // 转让：企业所有者更新为目标用户，原所有者降为 MEMBER
            Long oldOwnerId = company.getOwnerUserId();
            company.setOwnerUserId(approval.getTargetUserId());
            companyRepository.save(company);
            companyMemberRepository.findByCompanyIdAndUserId(company.getCompanyId(), oldOwnerId)
                    .ifPresent(m -> {
                        m.setRole(CompanyMemberRole.MEMBER);
                        companyMemberRepository.save(m);
                    });
            companyMemberRepository.findByCompanyIdAndUserId(company.getCompanyId(), approval.getTargetUserId())
                    .ifPresent(m -> {
                        m.setRole(CompanyMemberRole.OWNER);
                        companyMemberRepository.save(m);
                    });
        }
    }

    /** 待审批企业变更申请列表（按申请时间倒序） */
    public List<CompanyApproval> listPendingApprovals() {
        return companyApprovalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 查询企业，不存在抛异常 */
    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    /** 校验操作者为企业所有者 */
    private void ensureCompanyOwner(Company company, Long operatorUserId) {
        if (!company.getOwnerUserId().equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.NOT_COMPANY_OWNER);
        }
    }

    /** 同企业存在待审批申请则拒绝重复发起 */
    private void ensureNoPendingApproval(Long companyId) {
        if (companyApprovalRepository.existsByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING)) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_HANDLED);
        }
    }

    /** 审批人权限：系统管理员，或有权限的审计人员（审计范围覆盖该企业） */
    private boolean isApproverAuthorized(Long reviewerUserId, Long companyId) {
        SysUser reviewer = sysUserRepository.findById(reviewerUserId).orElse(null);
        if (reviewer == null || reviewer.getDeletedAt() != null) {
            return false;
        }
        if (reviewer.getSystemRole() == SystemRole.SYSTEM_ADMIN) {
            return true;
        }
        if (reviewer.getSystemRole() == SystemRole.AUDITOR) {
            return auditPermissionRepository.findByUserId(reviewerUserId)
                    .map(ap -> isAuditScopeCovers(ap, companyId))
                    .orElse(false);
        }
        return false;
    }

    /** 审计范围是否覆盖该企业 */
    private boolean isAuditScopeCovers(AuditPermission ap, Long companyId) {
        if (ap.getExpiredAt() != null && ap.getExpiredAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (ap.getAuditScope() == AuditScope.ALL) {
            return true;
        }
        if (ap.getAuditScope() != AuditScope.ENTERPRISE_USERS) {
            return false; // REGULAR_USERS 不覆盖企业
        }
        Map<String, Object> details = ap.getScopeDetails();
        if (details == null || details.get("allowed_company_ids") == null) {
            return true;
        }
        Object ids = details.get("allowed_company_ids");
        if (ids instanceof List<?> list) {
            return list.stream().map(String::valueOf)
                    .anyMatch(v -> v.equals(String.valueOf(companyId)));
        }
        return false;
    }
}


