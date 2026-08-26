package com.crm.repository;

import com.crm.common.enums.OwnerType;
import com.crm.common.enums.ResourceStatus;
import com.crm.entity.Resource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 统一资源实体表仓储。
 */
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceNo(String resourceNo);

    boolean existsByResourceNo(String resourceNo);

    List<Resource> findByParentResourceId(Long parentResourceId);

    List<Resource> findByParentResourceIdAndStatus(Long parentResourceId, ResourceStatus status);

    long countByParentResourceId(Long parentResourceId);

    /**
     * 统计某归属主体（用户 / 企业）名下所有有效资源的存储占用之和（字节，UC-029 存储配额）。
     * 通过 resource_owners 关联归属关系，仅统计 ACTIVE 资源的 file_size。
     */
    @Query("SELECT COALESCE(SUM(r.fileSize), 0) FROM Resource r "
            + "JOIN ResourceOwner ro ON ro.resourceId = r.resourceId "
            + "WHERE ro.ownerType = :ownerType AND ro.ownerId = :ownerId "
            + "AND r.status = :status")
    long sumFileSizeByOwner(@Param("ownerType") OwnerType ownerType,
                            @Param("ownerId") Long ownerId,
                            @Param("status") ResourceStatus status);
}
