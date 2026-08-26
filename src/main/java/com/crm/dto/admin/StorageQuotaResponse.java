package com.crm.dto.admin;

import com.crm.common.enums.OwnerType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 个体存储配额响应。
 */
@Data
public class StorageQuotaResponse {

    /** 主体类型 */
    private OwnerType quotaType;

    /** 主体 ID */
    private Long subjectId;

    /** 专属存储上限（字节） */
    private Long quotaBytes;

    /** 最近更新人用户 ID（系统管理员） */
    private Long updatedBy;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;
}
