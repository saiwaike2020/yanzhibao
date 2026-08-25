package com.crm.dto.resource;

import com.crm.common.enums.GranteeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

/**
 * 设置数据行级权限规则请求 (UC-013)。
 * 基于业务对象属性（如地区、客户等级）过滤数据行。
 */
@Data
public class RowPermissionRuleRequest {

    /** 授权主体类型：GROUP / USER */
    @NotNull(message = "授权主体类型不能为空")
    private GranteeType granteeType;

    /** 授权主体 ID */
    @NotNull(message = "授权主体 ID 不能为空")
    private Long granteeId;

    /** 规则类型（如 REGION / CUSTOMER_LEVEL 等） */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /** 过滤表达式（JSONB，业务对象属性条件） */
    private Map<String, Object> filterExpression;
}
