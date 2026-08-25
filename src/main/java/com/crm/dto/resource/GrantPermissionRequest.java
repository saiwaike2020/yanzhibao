package com.crm.dto.resource;

import com.crm.common.enums.GranteeType;
import com.crm.common.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分配资源权限请求（UC-011 授权给分组 / UC-012 授权给用户）。
 */
@Data
public class GrantPermissionRequest {

    /** 授权主体类型：GROUP（分组）/ USER（用户） */
    @NotNull(message = "授权主体类型不能为空")
    private GranteeType granteeType;

    /** 授权主体 ID（分组 ID 或用户 ID） */
    @NotNull(message = "授权主体 ID 不能为空")
    private Long granteeId;

    /** 权限级别：READ / WRITE / OWNER */
    @NotNull(message = "权限级别不能为空")
    private PermissionLevel permissionLevel;
}
