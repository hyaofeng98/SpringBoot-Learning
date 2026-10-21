package com.hyao.mybatis.model.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 订单支付请求.
 */
@Data
public class OrderPayRequest {

    /** 订单编号 */
    @NotBlank(message = "不能为空")
    private String orderId;

    /** 实际支付金额 */
    @NotNull(message = "不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal payAmount;
}
