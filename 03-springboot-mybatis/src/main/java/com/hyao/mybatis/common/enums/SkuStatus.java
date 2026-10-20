package com.hyao.mybatis.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 商品状态枚举.
 */
public enum SkuStatus implements BaseEnum {
    ON_SHELF(1, "上架"),
    OFF_SHELF(0, "下架");

    private final int code;
    private final String desc;

    SkuStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    @JsonValue
    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
