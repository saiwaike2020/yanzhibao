package com.crm.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用分页查询参数。
 */
@Data
public class PageQueryRequest {

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "page 不能小于 1")
    private int page = 1;

    /** 每页大小 */
    @Min(value = 1, message = "size 不能小于 1")
    @Max(value = 200, message = "size 不能大于 200")
    private int size = 20;

    /** 模糊搜索关键字（可选） */
    private String keyword;
}
