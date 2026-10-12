package com.hyao.advice.common;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * 自定义业务异常。
 */
@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    private final String errorMessage;

    public BizException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getMessage();
    }

    public BizException(ErrorCode errorCode, Object... message) {
        this.errorCode = errorCode;
        this.errorMessage = buildErrorMessage(errorCode, message);
    }

    private String buildErrorMessage(ErrorCode errorCode, Object... message) {
        return MessageFormat.format(errorCode.getMessage(), message);
    }
}
