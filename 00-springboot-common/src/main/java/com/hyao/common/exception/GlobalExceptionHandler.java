package com.hyao.common.exception;

import com.hyao.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理，用于把散落在各个 Controller 里的 try-catch 统一收口，返回结构化、可读的错误响应。
 *
 * <p>
 * 核心机制：{@code @RestControllerAdvice} + {@code @ExceptionHandler}。
 * <p>
 * {@code @RestControllerAdvice} 拦截指定包抛出的异常，命中{@code @ExceptionHandler}的方法后会直接返回其结果，
 * 这样就不用每个接口都写 try-catch。
 * 这里继承了 {@code ResponseEntityExceptionHandler}，可以统一处理 Spring Web 抛出的异常。
 *
 * <p>错误码分工见 {@link SysErrorCode}：业务异常返回 200 + 业务码；请求/参数层面的异常保留真实 HTTP 状态码，
 * 但响应体同样是 {@link Result}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 处理业务异常。
     * <ul><li>
     *     {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 多个 advice 之间的匹配规则是「按 @Order 排序后，取第一个能匹配该异常的
     *     方法」。注解中不定义 value 时默认优先级最低.
     * </li></ul>
     *
     * @param be 业务异常对象
     * @return 统一的返回体
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(BizException.class)
    public Result<Void> handleException(BizException be) {
        log.error("[{}] {}", be.getCode(), be.getMessage(), be);
        return Result.fail(be.getCode(), be.getMessage());
    }

    /**
     * 405：请求方法不支持（例如接口只接受 POST，却用了 GET）。
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                        HttpHeaders headers, HttpStatus status, WebRequest request) {
        Set<String> supported = ex.getSupportedHttpMethods() == null ? null
                : ex.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.toSet());
        String message = (supported == null || supported.isEmpty())
                ? SysErrorCode.METHOD_NOT_SUPPORTED.getMessage()
                : SysErrorCode.METHOD_NOT_SUPPORTED.getMessage() + "，该接口仅支持 " + String.join("/", supported);
        log.warn("[{}] {}", SysErrorCode.METHOD_NOT_SUPPORTED.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.METHOD_NOT_SUPPORTED.getCode(), message),
                headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 404：找不到处理该 URL 的 Handler。
     *
     * <p>只有开启 {@code spring.mvc.throw-exception-if-no-handler-found=true} 时才会走到这里；
     * 否则未匹配的 URL 会由容器的 {@code /error} 处理，由 {@code CommonErrorController} 兜底成同样的 {@link Result}。
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        String message = SysErrorCode.HANDLER_NOT_FOUND.getMessage() + ": " + ex.getHttpMethod() + " " + ex.getRequestURL();
        log.warn("[{}] {}", SysErrorCode.HANDLER_NOT_FOUND.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.HANDLER_NOT_FOUND.getCode(), message),
                headers, HttpStatus.NOT_FOUND);
    }

    /**
     * 400：{@code @Valid @RequestBody} 校验失败。
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        String message = formatFieldErrors(ex.getBindingResult().getAllErrors());
        log.warn("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：表单 / 查询参数绑定到对象后校验失败。
     */
    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers,
                                                         HttpStatus status, WebRequest request) {
        String message = formatFieldErrors(ex.getBindingResult().getAllErrors());
        log.warn("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：缺少必填的请求参数。
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                         HttpHeaders headers, HttpStatus status, WebRequest request) {
        String message = SysErrorCode.PARAM_INVALID.getMessage() + "，缺少参数: " + ex.getParameterName();
        log.warn("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：请求体解析失败（JSON 格式错误、字段类型不匹配等）。
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        String message = SysErrorCode.PARAM_INVALID.getMessage() + "，请求体解析失败";
        log.warn("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：类 / 方法上标了 {@code @Validated} 时，{@code @RequestParam}、{@code @PathVariable} 的约束校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .distinct().collect(Collectors.joining("; "));
        message = SysErrorCode.PARAM_INVALID.getMessage() + ": " + message;
        log.warn("[{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return ResponseEntity.badRequest().body(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 400：参数类型不匹配（例如 {@code pageSize=abc}）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName();
        String message = SysErrorCode.PARAM_TYPE_MISMATCH.getMessage() + ", " + ex.getName() + " 期望类型 " + expected;
        log.warn("[{}] {}", SysErrorCode.PARAM_TYPE_MISMATCH.getCode(), message);
        return ResponseEntity.badRequest().body(Result.fail(SysErrorCode.PARAM_TYPE_MISMATCH.getCode(), message));
    }

    /**
     * 重写 {@code ResponseEntityExceptionHandler.handleExceptionInternal} 方法，统一处理 Spring Web 抛出的其余异常。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} {}", status.value(), request.getDescription(false), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {} {}", status.value(), request.getDescription(false), ex.getMessage(), ex);
        }
        SysErrorCode errorCode = status.is4xxClientError() ? SysErrorCode.REQUEST_ERROR : SysErrorCode.SYSTEM_ERROR;
        return new ResponseEntity<>(Result.fail(errorCode.getCode(), errorCode.getMessage()), headers, status);
    }

    /**
     * 处理所有不可知的异常。
     *
     * @param e 异常对象
     * @return 统一的返回体
     */
    @Order
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.fail();
    }

    /**
     * 把字段错误拼成「字段名: 提示」，多个字段用分号隔开。
     */
    private static String formatFieldErrors(List<? extends ObjectError> fieldErrors) {
        String detail = fieldErrors.stream()
                .map(e -> {
                    String path = e instanceof FieldError ? ((FieldError) e).getField() : e.getObjectName();
                    return path + ": " + e.getDefaultMessage();
                })
                .distinct()
                .collect(Collectors.joining("; "));
        return detail.isEmpty()
                ? SysErrorCode.PARAM_INVALID.getMessage()
                : SysErrorCode.PARAM_INVALID.getMessage() + ": " + detail;
    }

}
