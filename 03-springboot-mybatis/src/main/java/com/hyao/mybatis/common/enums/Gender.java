package com.hyao.mybatis.common.enums;

import com.hyao.common.exception.BizException;
import com.hyao.mybatis.common.errorcode.BizErrorCode;
import lombok.Getter;

@Getter
public enum Gender implements BaseEnum {
    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String message;

    Gender(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Gender of(int code) {
        for (Gender gender : Gender.values()) {
            if (gender.getCode() == code) {
                return gender;
            }
        }
        throw new BizException(BizErrorCode.BIZ_ENUM_ERROR);
    }
}
