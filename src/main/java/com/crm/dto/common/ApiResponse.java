package com.crm.dto.common;

import lombok.Data;

/**
 * 统一 API 响应包装。
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> {

    /** 业务状态码：0 表示成功，非 0 表示业务错误 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
