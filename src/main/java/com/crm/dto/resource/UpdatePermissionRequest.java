package com.crm.dto.resource;

import com.crm.common.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 修改资源权限级别 / 有效期请求。
 */
@Data
public class UpdatePermissionRequest {

    /** 新的权限级别：READ / WRITE / OWNER */
    @NotNull(message = "权限级别不能为空")
    private PermissionLevel permissionLevel;

    /** 起始可用日期（必填） */
    @NotNull(message = "起始可用日期不能为空")
    private LocalDateTime validFrom;

    /** 过期时间（为空表示一直有效） */
    private LocalDateTime validUntil;
}
