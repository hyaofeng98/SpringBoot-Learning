package com.hyao.mybatis.common.enums;

import com.hyao.common.exception.BizException;
import com.hyao.common.exception.SysErrorCode;
import lombok.Getter;

@Getter
public enum UserStatus implements BaseEnum {
    NORMAL(0, "正常"),
    DISABLE(1, "禁用"),
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
        throw new BizException(SysErrorCode.SYSTEM_ERROR);
    }
}
