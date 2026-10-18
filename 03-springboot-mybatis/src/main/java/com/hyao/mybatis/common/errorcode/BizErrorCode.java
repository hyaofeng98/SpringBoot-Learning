package com.hyao.mybatis.common.errorcode;

import com.hyao.common.exception.IErrorCode;
import lombok.Getter;

@Getter
public enum BizErrorCode implements IErrorCode {
    DB_ERROR("DBE0001", "DB操作异常"),
    BIZ_ENUM_ERROR("BIZ0001", "业务枚举代码异常"),
    ;

    private final String code;

    private final String message;

    BizErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
