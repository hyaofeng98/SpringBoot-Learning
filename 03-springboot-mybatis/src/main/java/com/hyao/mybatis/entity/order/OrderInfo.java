package com.hyao.mybatis.entity.order;

import com.hyao.mybatis.common.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表 {@code order_info} 实体类。
 */
@Data
public class OrderInfo {

    /** 订单号（字符串主键，应用生成，定长且单调递增） */
    private String id;

    /** 下单用户id */
    private String userId;

    /** 订单状态：0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消 */
    private OrderStatus status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 收货人 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 取消时间（未取消为 1970-01-01） */
    private LocalDateTime cancelledAt;

    /** 订单明细列表 */
    private List<OrderItem> items;
}
