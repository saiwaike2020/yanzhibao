package com.crm.dto.resource;

import com.crm.common.enums.OwnerType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 登记资源所有者请求（新增/登记一个用户或企业为该资源的所有者）。
 * 起始可用日期必填；过期时间为空表示一直有效。
 */
@Data
public class GrantOwnershipRequest {

    /** 所有者类型：USER / COMPANY */
    @NotNull(message = "所有者类型不能为空")
    private OwnerType ownerType;

    /** 所有者 ID（用户 ID 或企业 ID） */
    @NotNull(message = "所有者 ID 不能为空")
    private Long ownerId;

    /** 起始可用日期（必填） */
    @NotNull(message = "起始可用日期不能为空")
    private LocalDateTime validFrom;

    /** 过期时间（可选，为空表示一直有效；不能早于起始日期） */
    private LocalDateTime validUntil;
}