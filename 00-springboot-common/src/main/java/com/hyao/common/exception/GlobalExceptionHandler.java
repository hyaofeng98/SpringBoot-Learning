package com.hyao.common.exception;

import com.hyao.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 全局异常处理，用于把散落在各个 Controller 里的 try-catch 统一收口，返回结构化、可读的错误响应。
 *
 * <p>
 * 核心机制：{@code @RestControllerAdvice} + {@code @ExceptionHandler}。
 * <p>
 * {@code @RestControllerAdvice} 拦截指定包抛出的异常，命中{@code @ExceptionHandler}的方法后会直接返回其结果，
 * 这样就不用每个接口都写 try-catch。
 * 这里继承了 {@code ResponseEntityExceptionHandler}，可以统一处理 Spring Web 抛出的异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param be 业务异常对象
     * @return 统一的返回体
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleException(BizException be) {
        log.warn("[{}] {}", be.getCode(), be.getMessage(), be);
        return Result.fail(be.getCode(), be.getMessage());
    }

    /**
     * 重写 {@code ResponseEntityExceptionHandler.handleExceptionInternal} 方法，统一处理 Spring Web 抛出的异常。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} {}", status.value(), request.getDescription(false), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {} {}", status.value(), request.getDescription(false), ex.getMessage());
        }
        ErrorCode errorCode = status.is4xxClientError() ? ErrorCode.REQUEST_ERROR : ErrorCode.SYSTEM_ERROR;
        return new ResponseEntity<>(Result.fail(errorCode.getCode(), errorCode.getMessage()), headers, status);
    }

    /**
     * 处理所有不可知的异常。
     * @param e 异常对象
     * @return 统一的返回体
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.fail();
    }

}
