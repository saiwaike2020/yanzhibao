package com.crm.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加分组成员请求 (UC-009)。
 */
@Data
public class AddGroupMemberRequest {

    /** 要加入分组的用户 ID（需为企业成员） */
    @NotNull(message = "用户不能为空")
    private Long userId;
}
