package com.crm.dto.user;

import com.crm.common.enums.SystemRole;
import com.crm.common.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户资料响应（sys_users）。
 */
@Data
public class UserProfileResponse {

    /** 用户 ID */
    private Long userId;

    /** 用户编号 */
    private String userNo;

    /** 手机号（掩码展示） */
    private String phoneMasked;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 用户状态 */
    private UserStatus status;

    /** 系统角色 */
    private SystemRole systemRole;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
