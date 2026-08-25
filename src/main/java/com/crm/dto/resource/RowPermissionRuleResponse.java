package com.crm.dto.resource;

import com.crm.common.enums.GranteeType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 数据行级权限规则响应（row_permission_rules）。
 */
@Data
public class RowPermissionRuleResponse {

    /** 规则 ID */
    private Long ruleId;

    /** 资源 ID */
    private Long resourceId;

    /** 授权主体类型 */
    private GranteeType granteeType;

    /** 授权主体 ID */
    private Long granteeId;

    /** 规则类型 */
    private String ruleType;

    /** 过滤表达式（JSONB） */
    private Map<String, Object> filterExpression;

    /** 状态（1-生效 0-停用） */
    private Integer status;

    /** 创建人用户 ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
