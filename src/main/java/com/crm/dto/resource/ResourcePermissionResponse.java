package com.crm.dto.resource;

import com.crm.common.enums.GranteeType;
import com.crm.common.enums.PermissionLevel;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资源权限记录响应（resource_permissions）。
 */
@Data
public class ResourcePermissionResponse {

    /** 权限记录 ID */
    private Long permissionId;

    /** 资源 ID */
    private Long resourceId;

    /** 授权主体类型 */
    private GranteeType granteeType;

    /** 授权主体 ID */
    private Long granteeId;

    /** 权限级别 */
    private PermissionLevel permissionLevel;

    /** 授权人用户 ID */
    private Long grantedBy;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
