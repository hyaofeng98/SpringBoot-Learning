package com.hyao.common.exception;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * 自定义业务异常。
 */
@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;

    private final String message;

    public BizException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BizException(ErrorCode errorCode, Object... message) {
        this.code = errorCode.getCode();
        this.message = formatErrorMessage(errorCode, message);
    }

    private String formatErrorMessage(ErrorCode errorCode, Object... message) {
        return MessageFormat.format(errorCode.getMessage(), message);
    }
}
