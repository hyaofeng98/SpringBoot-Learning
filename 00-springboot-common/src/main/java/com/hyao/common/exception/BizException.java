package com.hyao.common.exception;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * 自定义业务异常。
 *
 * @version 1.1 at 2026-10-13 22:42 新增IErrorCode接口并替换此类的方法签名，以便于扩展自定义业务错误码
 * @see IErrorCode
 */
@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;

    private final String message;

    public BizException(IErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BizException(IErrorCode errorCode, Object... message) {
        this.code = errorCode.getCode();
        this.message = formatErrorMessage(errorCode, message);
    }

    private String formatErrorMessage(IErrorCode errorCode, Object... message) {
        return MessageFormat.format(errorCode.getMessage(), message);
    }
}
