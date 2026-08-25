package com.crm.dto.resource;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

/**
 * 更新数据行级权限规则请求。
 */
@Data
public class UpdateRowPermissionRuleRequest {

    /** 规则类型 */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /** 过滤表达式（JSONB） */
    private Map<String, Object> filterExpression;
}
