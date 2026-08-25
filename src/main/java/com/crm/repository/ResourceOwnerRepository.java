package com.crm.repository;

import com.crm.common.enums.OwnerType;
import com.crm.entity.ResourceOwner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 资源所有者表仓储（多所有者、支持转让，含有效期）。
 */
public interface ResourceOwnerRepository extends JpaRepository<ResourceOwner, Long> {

    /** 某资源的全部所有权记录 */
    List<ResourceOwner> findByResourceId(Long resourceId);

    /** 某资源的指定主体（用户/企业）的所有权记录 */
    Optional<ResourceOwner> findByResourceIdAndOwnerTypeAndOwnerId(Long resourceId, OwnerType ownerType, Long ownerId);

    /** 按资源 + 所有权记录 ID 查询 */
    Optional<ResourceOwner> findByResourceIdAndOwnershipId(Long resourceId, Long ownershipId);

    /** 某主体（用户/企业）拥有的全部资源所有权 */
    List<ResourceOwner> findByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);

    boolean existsByResourceIdAndOwnerTypeAndOwnerId(Long resourceId, OwnerType ownerType, Long ownerId);

    long countByResourceIdAndStatus(Long resourceId, Integer status);
}