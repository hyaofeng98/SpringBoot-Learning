package com.hyao.mybatis.model.order;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 订单支付请求.
 */
@Data
public class OrderPayRequest {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderId;

    /** 实际支付金额 */
    @NotNull(message = "实际支付金额不能为空")
    @Positive(message = "实际支付金额必须大于 0")
    private BigDecimal payAmount;
}
