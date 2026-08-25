package com.crm.dto.delegation;

import com.crm.common.enums.DelegationScope;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理授权记录响应（management_delegations）。
 */
@Data
public class DelegationResponse {

    /** 授权 ID */
    private Long delegationId;

    /** 企业 ID */
    private Long companyId;

    /** 被管理的分组 ID */
    private Long groupId;

    /** 被授权用户 ID（分组管理员） */
    private Long granteeUserId;

    /** 授权人用户 ID */
    private Long grantedBy;

    /** 授权范围 */
    private DelegationScope scope;

    /** 状态（1-有效 0-已撤销） */
    private Integer status;

    /** 授权时间 */
    private LocalDateTime createdAt;
}
