package com.hyao.mybatis.common.enums;

import com.hyao.common.exception.BizException;
import com.hyao.mybatis.common.errorcode.BizErrorCode;
import lombok.Getter;

@Getter
public enum UserStatus implements BaseEnum {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    DELETED(2, "删除");

    private final int code;
    private final String message;

    UserStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static UserStatus of(int code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new BizException(BizErrorCode.BIZ_ENUM_ERROR);
    }
}
