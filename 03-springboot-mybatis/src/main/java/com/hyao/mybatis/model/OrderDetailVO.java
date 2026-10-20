package com.hyao.mybatis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hyao.mybatis.common.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应.
 */
@Data
public class OrderDetailVO {
    /** 订单编号 */
    private String id;

    /** 用户id */
    private String userId;

    /** 订单状态 */
    private OrderStatus orderStatus;

    /** 总计金额 */
    private BigDecimal totalAmount;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 收货人地址 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;

    /** 订单创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 订单更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 订单取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    /** 订单明细列表 */
    private List<OrderItemVO> items;
}
