package com.crm.entity;

import com.crm.common.enums.GroupMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 分组成员关联表 group_members。
 *
 * <p>唯一约束 (group_id, user_id)；组内角色通过 group_role 表达（MEMBER / GROUP_LEADER）。
 */
@Getter
@Setter
@Entity
@Table(name = "group_members")
public class GroupMember {

    /** 主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分组 ID */
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** 用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 组内角色：MEMBER 普通组员 / GROUP_LEADER 分组管理员 */
    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false, length = 20)
    private GroupMemberRole groupRole;

    /** 加入时间 */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (groupRole == null) {
            groupRole = GroupMemberRole.MEMBER;
        }
    }
}
