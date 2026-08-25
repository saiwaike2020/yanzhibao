package com.crm.dto.resource;

import com.crm.common.enums.OwnerType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 所有权转让请求 (UC-023)：将资源所有权转让给目标用户或企业。
 * 转让成功后，目标成为新所有者，操作者（原所有者）失去该资源所有权。
 * 起始可用日期必填；过期时间为空表示一直有效。
 */
@Data
public class TransferOwnershipRequest {

    /** 目标所有者类型：USER / COMPANY */
    @NotNull(message = "目标所有者类型不能为空")
    private OwnerType targetOwnerType;

    /** 目标所有者 ID（用户 ID 或企业 ID） */
    @NotNull(message = "目标所有者 ID 不能为空")
    private Long targetOwnerId;

    /** 目标起始可用日期（必填） */
    @NotNull(message = "起始可用日期不能为空")
    private LocalDateTime validFrom;

    /** 目标过期时间（可选，为空表示一直有效；不能早于起始日期） */
    private LocalDateTime validUntil;
}