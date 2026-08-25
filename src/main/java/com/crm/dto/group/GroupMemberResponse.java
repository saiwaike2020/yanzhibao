package com.crm.dto.group;

import com.crm.common.enums.GroupMemberRole;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分组成员响应（group_members）。
 */
@Data
public class GroupMemberResponse {

    /** 分组成员关系 ID */
    private Long groupMemberId;

    /** 分组 ID */
    private Long groupId;

    /** 用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 昵称 */
    private String nickname;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 组内角色 */
    private GroupMemberRole role;

    /** 加入时间 */
    private LocalDateTime joinedAt;
}
