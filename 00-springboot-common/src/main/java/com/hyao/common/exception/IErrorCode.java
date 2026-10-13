package com.hyao.common.exception;

/**
 * 错误码公共接口。自定义的错误码枚举类实现这个接口，详见 {@link SysErrorCode}。。
 */
public interface IErrorCode {

    String getCode();

    String getMessage();
}
