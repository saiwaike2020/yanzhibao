package com.crm.dto.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审计日志查询参数。
 */
@Data
public class AuditLogQueryRequest {

    /** 按用户 ID 过滤（可选） */
    private Long userId;

    /** 按企业 ID 过滤（可选） */
    private Long companyId;

    /** 按操作过滤（可选） */
    private String action;

    /** 开始时间（可选） */
    private LocalDateTime startTime;

    /** 结束时间（可选） */
    private LocalDateTime endTime;

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "page 不能小于 1")
    private int page = 1;

    /** 每页大小 */
    @Min(value = 1, message = "size 不能小于 1")
    @Max(value = 200, message = "size 不能大于 200")
    private int size = 20;
}
