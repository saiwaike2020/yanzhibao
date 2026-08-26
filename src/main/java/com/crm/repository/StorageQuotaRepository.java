package com.crm.repository;

import com.crm.common.enums.OwnerType;
import com.crm.entity.StorageQuota;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 个体存储配额表仓储。
 */
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    /** 查询某主体（用户 / 企业）的专属存储配额 */
    Optional<StorageQuota> findByQuotaTypeAndSubjectId(OwnerType quotaType, Long subjectId);
}
