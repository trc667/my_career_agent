package com.example.aimaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 返回值封装。
 *
 * @param <T> data 类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务码：200 成功，400 参数错误，500 服务异常 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据 */
    private T data;

    public static <T> Result<T> ok(T data) {
        return Result.<T>builder().code(200).message("success").data(data).build();
    }

    public static <T> Result<T> ok(String message, T data) {
        return Result.<T>builder().code(200).message(message).data(data).build();
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder().code(code).message(message).data(null).build();
    }

    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }
}
