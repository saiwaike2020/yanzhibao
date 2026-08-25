package com.crm.repository;

import com.crm.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 企业分组表仓储。
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCompanyId(Long companyId);

    Optional<Group> findByCompanyIdAndGroupId(Long companyId, Long groupId);

    long countByCompanyId(Long companyId);
}
