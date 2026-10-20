package com.hyao.mybatis.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class OrderPayRequest {

    @NotBlank(message = "不能为空")
    private String orderId;

    @NotNull(message = "不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal payAmount;
}
