package com.crm.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批企业变更申请请求（UC-033：企业注销 / 所有权转让）。
 */
@Data
public class ReviewCompanyApprovalRequest {

    /** 是否批准：true 批准，false 拒绝 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批意见（可选） */
    private String note;
}
