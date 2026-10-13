package com.hyao.common.exception;

import lombok.Getter;

/**
 * 系统错误码枚举类。
 *
 * @version 1.1 at 2026-10-13 22:43 新增{@code IErrorCode}接口并实现，以便于扩展自定义业务错误码
 * @see IErrorCode
 */
@Getter
public enum SysErrorCode implements IErrorCode {
    SYSTEM_ERROR("SYS0001", "系统内部异常"),
    REQUEST_ERROR("SYS0002", "请求异常");

    private final String code;
    private final String message;

    SysErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
