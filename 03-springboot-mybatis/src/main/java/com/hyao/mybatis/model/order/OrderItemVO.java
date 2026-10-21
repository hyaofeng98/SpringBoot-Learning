package com.hyao.mybatis.model.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细响应.
 */
@Data
public class OrderItemVO {
    /** 订单明细编号 */
    private Long id;

    /** SKU 编号 */
    private String skuId;

    /** 商品名称 */
    private String skuName;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 金额 */
    private BigDecimal amount;
}
