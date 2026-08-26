package com.crm.dto.resource;

import com.crm.common.enums.GranteeType;
import com.crm.common.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分配资源权限请求（UC-011 授权给分组 / UC-012 授权给用户）。
 *
 * <p>有效期：起始可用日期 validFrom 必填；过期时间 validUntil 可选，为空表示一直有效。
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

    /** 起始可用日期（必填） */
    @NotNull(message = "起始可用日期不能为空")
    private LocalDateTime validFrom;

    /** 过期时间（为空表示一直有效） */
    private LocalDateTime validUntil;
}
