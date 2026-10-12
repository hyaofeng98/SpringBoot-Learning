package com.hyao.advice.common;

import lombok.Getter;

/**
 * 错误码枚举类。
 */
@Getter
public enum ErrorCode {

    QUERY_ERROR("QRY0001", "查询异常: {0}"),

    SYSTEM_ERROR("SYS0001", "系统内部异常"),

    REQUEST_ERROR("SYS0002", "请求异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
