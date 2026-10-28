package com.hyao.common.exception;

import com.hyao.common.model.Result;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理，用于把散落在各个 Controller 里的 try-catch 统一收口，返回结构化、可读的错误响应。
 *
 * <p>
 * 核心机制：{@code @RestControllerAdvice} + {@code @ExceptionHandler}。
 * <p>
 * 这里继承了 {@code ResponseEntityExceptionHandler}，通过覆盖它预留的钩子统一接管 Spring Web 自身抛出的异常。
 * <p>错误码分工见 {@link SysErrorCode}：业务异常返回 200 + 业务码；请求/参数层面的异常保留真实 HTTP 状态码，
 * 但响应体同样是 {@link Result}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 处理业务异常：直接返回 200 + 业务码（不占用 HTTP 错误码）。
     *
     * @param be 业务异常对象
     * @return 统一的返回体
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleException(BizException be) {
        log.error("BizException: [{}] {}", be.getCode(), be.getMessage(), be);
        return Result.fail(be.getCode(), be.getMessage());
    }

    /**
     * 405：请求方法不支持（例如接口只接受 POST，却用了 GET）。
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                        HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Set<String> supported = ex.getSupportedHttpMethods() == null ? null
                : ex.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.toSet());
        String message = (supported == null || supported.isEmpty())
                ? SysErrorCode.METHOD_NOT_SUPPORTED.getMessage()
                : SysErrorCode.METHOD_NOT_SUPPORTED.getMessage() + "，该接口仅支持 " + String.join("/", supported);
        log.warn("HttpRequestMethodNotSupportedException: [{}] {}", SysErrorCode.METHOD_NOT_SUPPORTED.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.METHOD_NOT_SUPPORTED.getCode(), message),
                headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 400：{@code @RequestBody} 上的 {@code @Valid} / {@code @Validated} 校验失败。
     *
     * <p>异常在参数解析阶段就抛出，还没进入 Controller 方法体，所以能拿到完整的 {@code BindingResult}。
     * {@code defaultMessage} 里只有 message 内容、不带字段路径，注解的 message 要自己写清字段含义。
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String detail = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        String message = StringUtils.isBlank(detail)
                ? SysErrorCode.PARAM_INVALID.getMessage()
                : SysErrorCode.PARAM_INVALID.getMessage() + ": " + detail;
        log.warn("MethodArgumentNotValidException: [{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：Spring MVC 6.1+ 内置参数校验失败，抛 {@link HandlerMethodValidationException}。
     * <p><b>注意:</b>注解的 message 要自己写清字段含义.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                           HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<? extends MessageSourceResolvable> allErrors = ex.getAllErrors();
        String message = allErrors.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        message = SysErrorCode.PARAM_INVALID.getMessage() + ": " + message;
        log.warn("HandlerMethodValidationException: [{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：缺少必填的请求参数。
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                         HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = SysErrorCode.PARAM_INVALID.getMessage() + "，缺少参数: " + ex.getParameterName();
        log.warn("MissingServletRequestParameterException: [{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：请求体解析失败（JSON 格式错误、字段类型不匹配等）。
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String message = SysErrorCode.PARAM_INVALID.getMessage() + "，请求体解析失败";
        log.warn("HttpMessageNotReadableException: [{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return new ResponseEntity<>(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message),
                headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * 400：类 / 方法上标了 {@code @Validated} 时，方法参数（{@code @RequestParam}、{@code @PathVariable} 等）的约束校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct().collect(Collectors.joining("; "));
        message = SysErrorCode.PARAM_INVALID.getMessage() + ": " + message;
        log.warn("ConstraintViolationException: [{}] {}", SysErrorCode.PARAM_INVALID.getCode(), message);
        return ResponseEntity.badRequest().body(Result.fail(SysErrorCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 400：参数类型不匹配（例如 {@code pageSize=abc}）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName();
        String message = SysErrorCode.PARAM_TYPE_MISMATCH.getMessage() + ", " + ex.getName() + " 期望类型 " + expected;
        log.warn("MethodArgumentTypeMismatchException: [{}] {}", SysErrorCode.PARAM_TYPE_MISMATCH.getCode(), message);
        return ResponseEntity.badRequest().body(Result.fail(SysErrorCode.PARAM_TYPE_MISMATCH.getCode(), message));
    }

    /**
     * 兜住 {@code ResponseEntityExceptionHandler} 内部没有被单独覆盖的 Spring Web 异常。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("handleExceptionInternal-5xx: [{}] {} {}", status.value(), request.getDescription(false), ex.getMessage(), ex);
        } else {
            log.warn("handleExceptionInternal-other: [{}] {} {}", status.value(), request.getDescription(false), ex.getMessage(), ex);
        }
        SysErrorCode errorCode = status.is4xxClientError() ? SysErrorCode.REQUEST_ERROR : SysErrorCode.SYSTEM_ERROR;
        return new ResponseEntity<>(Result.fail(errorCode.getCode(), errorCode.getMessage()), headers, status);
    }

    /**
     * 兜底：处理所有没有被上面任何 {@code @ExceptionHandler} 命中的异常，返回 HTTP 200 + {@link SysErrorCode#SYSTEM_ERROR}。
     *
     * @param e 异常对象
     * @return 统一的返回体
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Exception: 系统内部异常", e);
        return Result.fail();
    }
}
