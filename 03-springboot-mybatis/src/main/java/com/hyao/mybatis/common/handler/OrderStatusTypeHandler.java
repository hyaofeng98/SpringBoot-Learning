package com.hyao.mybatis.common.handler;

import com.hyao.mybatis.common.enums.OrderStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 订单状态类型处理器.
 */
@MappedTypes(OrderStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class OrderStatusTypeHandler extends BaseEnumTypeHandler<OrderStatus>{

    public OrderStatusTypeHandler(Class<OrderStatus> type) {
        super(type);
    }
}
