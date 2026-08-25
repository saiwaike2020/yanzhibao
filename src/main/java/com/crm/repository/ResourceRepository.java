package com.crm.repository;

import com.crm.common.enums.ResourceStatus;
import com.crm.entity.Resource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 统一资源实体表仓储。
 */
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceNo(String resourceNo);

    boolean existsByResourceNo(String resourceNo);

    List<Resource> findByParentResourceId(Long parentResourceId);

    List<Resource> findByParentResourceIdAndStatus(Long parentResourceId, ResourceStatus status);

    long countByParentResourceId(Long parentResourceId);
}
