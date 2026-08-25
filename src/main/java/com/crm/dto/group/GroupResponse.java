package com.crm.dto.group;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分组信息响应（groups）。
 */
@Data
public class GroupResponse {

    /** 分组 ID */
    private Long groupId;

    /** 所属企业 ID */
    private Long companyId;

    /** 父分组 ID（当前一级分组为 null） */
    private Long parentGroupId;

    /** 分组名称 */
    private String name;

    /** 分组描述 */
    private String description;

    /** 状态（1-正常 0-禁用） */
    private Integer status;

    /** 创建人用户 ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
