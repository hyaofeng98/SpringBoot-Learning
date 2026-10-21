package com.hyao.mybatis.model.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单统计项视图对象。
 */
@Data
public class OrderStatisticsItemVO {
    /** day: yyyy-MM-dd; status: 0~4 */
    private String statKey;

    /** 仅 status 维度 */
    private String statusDesc;

    /** 订单数量 */
    private Long orderCount;

    /** 总计金额 */
    private BigDecimal totalAmount;
}
