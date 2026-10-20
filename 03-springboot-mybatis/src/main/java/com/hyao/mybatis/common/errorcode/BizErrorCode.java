package com.hyao.mybatis.common.errorcode;

import com.hyao.common.exception.IErrorCode;
import lombok.Getter;

/**
 * 实现{@code IErrorCode}接口, 以接入统一异常处理.
 */
@Getter
public enum BizErrorCode implements IErrorCode {
    DB_ERROR("DBE0001", "DB操作异常"),
    BIZ_ENUM_ERROR("BIZ0001", "业务枚举代码异常"),
    ORDER_NOT_FOUND("ORD0001", "订单不存在: {0}"),
    ORDER_STATUS_INVALID("ORD0002", "订单状态不允许该操作: {0}"),
    ORDER_ITEM_EMPTY("ORD0003", "订单明细不能为空"),
    ORDER_DUPLICATE("ORD0004", "订单重复提交: {0}"),
    ORDER_STATUS_UPDATE_FAILURE("ORD0005", "订单状态更新失败, 不存在{0}订单: {1}"),
    ORDER_CANCELLED_FAILURE("ORD0006", "订单取消失败, 不存在待支付或已支付订单: {0}"),
    SKU_NOT_FOUND("SKU0001", "商品不存在: {0}"),
    SKU_OFF_SHELF("SKU0002", "商品已下架: {0}"),
    SKU_STOCK_NOT_ENOUGH("SKU0003", "库存不足: {0}"),
    ;

    private final String code;

    private final String message;

    BizErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
