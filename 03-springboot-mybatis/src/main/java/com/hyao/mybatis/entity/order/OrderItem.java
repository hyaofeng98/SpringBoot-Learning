package com.hyao.mybatis.entity.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表 {@code order_item} 实体类。
 */
@Data
public class OrderItem {
    /** 明细id */
    private Long id;

    /** 订单号(order_info.id) */
    private String orderId;

    /** SKU编号 */
    private String skuId;

    /** 商品名称(快照) */
    private String skuName;

    /** 单价(快照) */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal amount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
