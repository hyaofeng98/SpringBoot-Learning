package com.hyao.common.model;

import com.hyao.common.exception.ErrorCode;
import lombok.Data;

/**
 * 统一的接口响应结果。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {
    private static final String SUCCESS_CODE = "SUC0000";

    /** 状态码，SUC0000 表示成功 */
    private String code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return build(SUCCESS_CODE, "操作成功", data);
    }

    public static <T> Result<T> fail() {
        return fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    public static <T> Result<T> fail(String code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(String code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
