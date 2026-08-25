package com.crm.dto.resource;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 调整所有权有效期请求 (UC-024)。
 * 起始可用日期必填；过期时间为空表示一直有效。
 */
@Data
public class UpdateOwnershipValidityRequest {

    /** 起始可用日期（必填） */
    @NotNull(message = "起始可用日期不能为空")
    private LocalDateTime validFrom;

    /** 过期时间（可选，为空表示一直有效；不能早于起始日期） */
    private LocalDateTime validUntil;
}