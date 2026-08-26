package com.crm.service;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.common.enums.CompanyStatus;
import com.crm.common.enums.MemberStatus;
import com.crm.common.exception.BusinessException;
import com.crm.common.exception.ErrorCode;
import com.crm.dto.company.ChangeMemberRoleRequest;
import com.crm.dto.company.CompanyMemberResponse;
import com.crm.dto.company.InviteMemberRequest;
import com.crm.dto.common.PageQueryRequest;
import com.crm.dto.common.PageResponse;
import com.crm.entity.Company;
import com.crm.entity.CompanyMember;
import com.crm.entity.SysUser;
import com.crm.repository.CompanyMemberRepository;
import com.crm.repository.CompanyRepository;
import com.crm.repository.SysUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业成员服务（邀请、申请加入、角色变更、禁用 / 恢复、移除、退出）。
 *
 * <p>消息联动：申请加入时向企业全部管理员发送 {@code JOIN_REQUEST} 消息（UC-026）；
 * 邀请加入时向被邀请用户发送 {@code INVITATION} 消息（UC-027）。
 * 当前不通过短信发送额外提醒，预留 {@code sms_notified} 扩展点。
 */
@Service
@RequiredArgsConstructor
public class CompanyMemberService {

    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyRepository companyRepository;
    private final SysUserRepository sysUserRepository;
    private final MessageService messageService;

    /**
     * 邀请成员加入企业 (UC-006 / UC-027)。
     *
     * <p>校验操作者为企业管理员、被邀请人已注册且未在该企业中；
     * 创建 INVITED 成员记录，并向被邀请用户发送 INVITATION 消息。
     */
    @Transactional
    public void inviteMember(Long companyId, Long operatorUserId, InviteMemberRequest request) {
        Company company = findActiveCompany(companyId);
        ensureCompanyAdmin(companyId, operatorUserId);

        // 被邀请人必须已注册（当前无短信通道，未注册用户暂无法接收站内邀请）
        SysUser targetUser = sysUserRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_REGISTERED));
        if (companyMemberRepository.existsByCompanyIdAndUserId(companyId, targetUser.getUserId())) {
            throw new BusinessException(ErrorCode.INVITE_TARGET_EXISTS);
        }

        CompanyMember invited = new CompanyMember();
        invited.setCompanyId(companyId);
        invited.setUserId(targetUser.getUserId());
        invited.setRole(CompanyMemberRole.MEMBER);
        invited.setStatus(MemberStatus.INVITED);
        companyMemberRepository.save(invited);

        // 消息联动：向被邀请用户发送邀请消息（站内，暂不短信提醒）
        messageService.sendInvitationMessage(targetUser.getUserId(), operatorUserId, companyId, company.getName());
    }

    /**
     * 用户申请加入企业 (UC-026)。
     *
     * <p>创建 INVITED 成员记录（待企业管理员审批），并向企业全部管理员发送 JOIN_REQUEST 消息。
     */
    @Transactional
    public void applyJoinCompany(Long companyId, Long applicantUserId) {
        Company company = findActiveCompany(companyId);
        if (companyMemberRepository.existsByCompanyIdAndUserId(companyId, applicantUserId)) {
            throw new BusinessException(ErrorCode.INVITE_TARGET_EXISTS);
        }

        CompanyMember apply = new CompanyMember();
        apply.setCompanyId(companyId);
        apply.setUserId(applicantUserId);
        apply.setRole(CompanyMemberRole.MEMBER);
        apply.setStatus(MemberStatus.INVITED);
        companyMemberRepository.save(apply);

        // 消息联动：向企业全部管理员（OWNER / ADMIN，状态 ACTIVE）发送申请消息
        List<CompanyMember> admins = companyMemberRepository.findByCompanyId(companyId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .filter(m -> m.getRole() == CompanyMemberRole.OWNER || m.getRole() == CompanyMemberRole.ADMIN)
                .toList();
        for (CompanyMember admin : admins) {
            messageService.sendJoinRequestMessage(admin.getUserId(), applicantUserId, companyId, company.getName());
        }
    }

    /**
     * 批准加入申请（UC-032）：成员状态 INVITED → ACTIVE。
     * 由企业所有者或管理员操作，并向申请用户发送结果消息。
     */
    @Transactional
    public void approveJoinRequest(Long companyId, Long operatorUserId, Long memberId) {
        ensureCompanyAdmin(companyId, operatorUserId);
        CompanyMember member = companyMemberRepository.findByCompanyIdAndMemberId(companyId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_HANDLED);
        }
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedAt(LocalDateTime.now());
        companyMemberRepository.save(member);

        messageService.sendSystemMessage(member.getUserId(), "企业加入申请已批准",
                "您申请加入的企业已批准您的申请，现已正式成为企业成员。");
    }

    /**
     * 拒绝加入申请（UC-032）：成员状态 INVITED → EXITED。
     * 由企业所有者或管理员操作，并向申请用户发送结果消息。
     */
    @Transactional
    public void rejectJoinRequest(Long companyId, Long operatorUserId, Long memberId) {
        ensureCompanyAdmin(companyId, operatorUserId);
        CompanyMember member = companyMemberRepository.findByCompanyIdAndMemberId(companyId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_HANDLED);
        }
        member.setStatus(MemberStatus.EXITED);
        companyMemberRepository.save(member);

        messageService.sendSystemMessage(member.getUserId(), "企业加入申请被拒绝",
                "您申请加入的企业拒绝了您的申请。");
    }

    /** 成员列表 */
    public PageResponse<CompanyMemberResponse> listMembers(Long companyId, PageQueryRequest query) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 成员详情 */
    public CompanyMemberResponse getMember(Long companyId, Long memberId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 变更企业成员角色（设置 / 取消管理员，短信验证，UC-022） */
    public void changeMemberRole(Long companyId, Long memberId, ChangeMemberRoleRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 禁用成员 */
    public void disableMember(Long companyId, Long memberId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 恢复成员 */
    public void restoreMember(Long companyId, Long memberId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 移除成员 / 撤销权限 (UC-016) */
    public void removeMember(Long companyId, Long memberId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** 用户退出企业 (UC-017) */
    public void leaveCompany(Long companyId, Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    // -------------------------------------------------------------------------
    // 私有方法
    // -------------------------------------------------------------------------

    /** 查询 ACTIVE 状态企业，不存在 / 已解散则抛异常 */
    private Company findActiveCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMPANY_DISSOLVED);
        }
        return company;
    }

    /** 校验操作者为该企业的有效管理员（OWNER / ADMIN） */
    private void ensureCompanyAdmin(Long companyId, Long operatorUserId) {
        CompanyMember operator = companyMemberRepository.findByCompanyIdAndUserId(companyId, operatorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (operator.getStatus() != MemberStatus.ACTIVE
                || (operator.getRole() != CompanyMemberRole.OWNER && operator.getRole() != CompanyMemberRole.ADMIN)) {
            throw new BusinessException(ErrorCode.NOT_COMPANY_ADMIN);
        }
    }
}
