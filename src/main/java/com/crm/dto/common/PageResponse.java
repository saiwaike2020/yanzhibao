package com.crm.dto.common;

import java.util.List;
import lombok.Data;

/**
 * 统一分页响应。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResponse<T> {

    /** 当前页数据 */
    private List<T> items;

    /** 总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页大小 */
    private int size;

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        PageResponse<T> resp = new PageResponse<>();
        resp.setItems(items);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }
}
