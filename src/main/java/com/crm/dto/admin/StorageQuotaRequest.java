package com.crm.dto.admin;

import com.crm.common.enums.OwnerType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置 / 调整个体存储配额请求（UC-031）。
 */
@Data
public class StorageQuotaRequest {

    /** 主体类型：USER（用户）/ COMPANY（企业） */
    @NotNull(message = "主体类型不能为空")
    private OwnerType quotaType;

    /** 主体 ID（用户 ID 或企业 ID） */
    @NotNull(message = "主体 ID 不能为空")
    private Long subjectId;

    /** 专属存储上限（字节），必须为正整数 */
    @NotNull(message = "存储配额不能为空")
    private Long quotaBytes;
}
