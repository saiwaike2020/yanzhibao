package com.crm.dto.company;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转让企业所有权请求（仅企业所有者可操作）。
 */
@Data
public class TransferCompanyRequest {

    /** 新所有者用户 ID（需为企业活跃成员） */
    @NotNull(message = "新所有者不能为空")
    private Long newOwnerUserId;
}
