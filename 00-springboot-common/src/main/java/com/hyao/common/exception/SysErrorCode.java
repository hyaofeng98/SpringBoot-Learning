package com.hyao.common.exception;

import lombok.Getter;

/**
 * 系统错误码枚举类。
 *
 * <p>约定：{@code SYS0001} 是服务端异常；{@code SYS0002}~{@code SYS0005} 是请求/参数层面的框架异常
 * （对应 HTTP 4xx，会保留真实状态码）；{@code SYS0006} 是其余 4xx 的兜底。
 *
 * @version 1.1 at 2026-10-13 22:43 新增{@code IErrorCode}接口并实现，以便于扩展自定义业务错误码
 * @version 1.2 at 2026-10-18 细化请求异常：拆出「方法不支持 / 接口不存在 / 参数校验失败 / 参数类型不匹配」
 * @see IErrorCode
 */
@Getter
public enum SysErrorCode implements IErrorCode {
    SYSTEM_ERROR("SYS0001", "系统内部异常"),
    METHOD_NOT_SUPPORTED("SYS0002", "请求方法不支持"),
    HANDLER_NOT_FOUND("SYS0003", "接口不存在"),
    PARAM_INVALID("SYS0004", "参数校验失败"),
    PARAM_TYPE_MISMATCH("SYS0005", "参数类型不匹配"),
    REQUEST_ERROR("SYS0006", "请求异常");

    private final String code;
    private final String message;

    SysErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
