package com.hyao.mybatis.mapper.order;

import com.hyao.mybatis.entity.order.OrderItem;

import java.util.List;

/**
 * 订单明细表(order_item) Mapper
 */
public interface OrderItemMapper {
    /**
     * 批量插入订单明细. 演示动态 SQL {@code <foreach>}
     *
     * @param items 订单明细列表
     * @return 受影响行数
     */
    int batchInsert(List<OrderItem> items);

    /**
     * 根据订单 ID 查询订单明细.
     *
     * @param orderId 订单编号
     * @return 订单明细列表
     */
    List<OrderItem> queryByOrderId(String orderId);
}
