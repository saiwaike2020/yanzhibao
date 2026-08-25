package com.crm.repository;

import com.crm.common.enums.GranteeType;
import com.crm.entity.RowPermissionRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 数据行级权限规则表仓储。
 */
public interface RowPermissionRuleRepository extends JpaRepository<RowPermissionRule, Long> {

    List<RowPermissionRule> findByResourceId(Long resourceId);

    Optional<RowPermissionRule> findByResourceIdAndRuleId(Long resourceId, Long ruleId);

    boolean existsByResourceIdAndGranteeTypeAndGranteeId(Long resourceId, GranteeType granteeType, Long granteeId);

    List<RowPermissionRule> findByGranteeTypeAndGranteeId(GranteeType granteeType, Long granteeId);
}
