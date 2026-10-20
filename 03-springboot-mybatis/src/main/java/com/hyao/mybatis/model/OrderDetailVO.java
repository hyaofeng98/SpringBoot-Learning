package com.hyao.mybatis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hyao.mybatis.common.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {
    private String id;

    private String userId;

    private OrderStatus orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    private List<OrderItemVO> items;
}
