package com.crm.repository;

import com.crm.common.enums.CompanyMemberRole;
import com.crm.entity.CompanyMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 企业成员关联表仓储。
 */
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    List<CompanyMember> findByCompanyId(Long companyId);

    /** 用户加入的所有企业成员关系 */
    List<CompanyMember> findByUserId(Long userId);

    Optional<CompanyMember> findByCompanyIdAndUserId(Long companyId, Long userId);

    Optional<CompanyMember> findByCompanyIdAndMemberId(Long companyId, Long memberId);

    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);

    long countByCompanyIdAndRole(Long companyId, CompanyMemberRole role);
}
