package com.hyao.mybatis.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long id;

    private String skuId;

    private String skuName;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;
}
