package com.hyao.advice.common;

import com.hyao.common.exception.IErrorCode;
import lombok.Getter;

/**
 * 自定义业务错误码枚举类。
 *
 * @version 1.0 at 2026-10-13 22:43 自定义的业务错误码枚举类必须实现 {@link IErrorCode} 接口
 * @see com.hyao.common.exception.IErrorCode
 * @see com.hyao.common.exception.BizException
 */
@Getter
public enum BizErrorCode implements IErrorCode {
    QUERY_ERROR("QRY0001", "查询错误: {0}"), // 演示错误信息占位符
    FILE_DOWNLOAD_ERROR("FDL0001", "文件下载异常"),
    ;

    private final String code;
    private final String message;

    BizErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
