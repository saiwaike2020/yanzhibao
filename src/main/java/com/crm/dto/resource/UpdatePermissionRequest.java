package com.crm.dto.resource;

import com.crm.common.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改资源权限级别请求。
 */
@Data
public class UpdatePermissionRequest {

    /** 新的权限级别：READ / WRITE / OWNER */
    @NotNull(message = "权限级别不能为空")
    private PermissionLevel permissionLevel;
}
