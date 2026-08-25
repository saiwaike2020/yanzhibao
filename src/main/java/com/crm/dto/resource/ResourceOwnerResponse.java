package com.crm.dto.resource;

import com.crm.common.enums.OwnerType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资源所有者记录响应（resource_owners）。
 */
@Data
public class ResourceOwnerResponse {

    /** 所有权记录 ID */
    private Long ownershipId;

    /** 资源 ID */
    private Long resourceId;

    /** 所有者类型：USER / COMPANY */
    private OwnerType ownerType;

    /** 所有者 ID（用户 ID 或企业 ID） */
    private Long ownerId;

    /** 起始可用日期（必填） */
    private LocalDateTime validFrom;

    /** 过期时间（为空表示一直有效） */
    private LocalDateTime validUntil;

    /** 授权/转让来源用户 ID */
    private Long grantedBy;

    /** 状态：1-有效 0-已撤销/已转让 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}