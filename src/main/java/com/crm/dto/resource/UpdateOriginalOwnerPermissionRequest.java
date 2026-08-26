package com.crm.dto.resource;

import com.crm.common.enums.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 企业管理员调整原所有者访问权限请求（UC-034，v3.6）。
 * 目标级别：NONE（无权）/ READ（只读）/ WRITE（可写）。
 */
@Data
public class UpdateOriginalOwnerPermissionRequest {

    /** 原所有者目标访问级别：NONE / READ / WRITE */
    @NotNull(message = "访问级别不能为空")
    private AccessLevel permissionLevel;
}
