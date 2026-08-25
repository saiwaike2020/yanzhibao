package com.crm.repository;

import com.crm.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 分组成员关联表仓储。
 */
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByIdAndGroupId(Long id, Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    /** 用户加入的所有分组 */
    List<GroupMember> findByUserId(Long userId);
}
