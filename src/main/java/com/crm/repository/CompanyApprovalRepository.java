package com.crm.repository;

import com.crm.common.enums.ApprovalStatus;
import com.crm.entity.CompanyApproval;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 企业变更审批表仓储。
 */
public interface CompanyApprovalRepository extends JpaRepository<CompanyApproval, Long> {

    /** 查询指定状态的审批记录（按申请时间倒序） */
    List<CompanyApproval> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);

    /** 某企业是否已有指定状态的审批记录 */
    boolean existsByCompanyIdAndStatus(Long companyId, ApprovalStatus status);
}
