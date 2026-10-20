package com.hyao.mybatis.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单状态枚举.
 */
public enum OrderStatus implements BaseEnum {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    DELIVERED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
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
