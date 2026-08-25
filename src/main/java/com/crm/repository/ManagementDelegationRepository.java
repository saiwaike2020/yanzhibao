package com.crm.repository;

import com.crm.entity.ManagementDelegation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 分级管理授权表仓储。
 */
public interface ManagementDelegationRepository extends JpaRepository<ManagementDelegation, Long> {

    List<ManagementDelegation> findByCompanyId(Long companyId);

    Optional<ManagementDelegation> findByCompanyIdAndDelegationId(Long companyId, Long delegationId);

    Optional<ManagementDelegation> findByGroupIdAndGranteeUserIdAndStatus(Long groupId, Long granteeUserId, Integer status);

    boolean existsByGroupIdAndGranteeUserIdAndStatus(Long groupId, Long granteeUserId, Integer status);

    /** 用户被授权的分组管理记录 */
    List<ManagementDelegation> findByGranteeUserId(Long granteeUserId);
}
