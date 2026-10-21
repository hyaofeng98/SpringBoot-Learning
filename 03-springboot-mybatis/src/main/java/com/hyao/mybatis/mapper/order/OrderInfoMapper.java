package com.hyao.mybatis.mapper.order;

import com.hyao.mybatis.entity.order.OrderInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单信息表(order_info) Mapper.
 */
public interface OrderInfoMapper {
    /**
     * 插入订单.
     *
     * @param orderInfo 订单信息
     * @return 影响行数
     */
    int insert(OrderInfo orderInfo);

    /**
     * 查询订单详情. 用于演示 {@code <resultMap>} 的使用.
     *
     * @param id 订单编号
     * @return 订单详情
     */
    OrderInfo queryDetail(String id);

    /**
     * 分页查询订单列表. 用于演示<b>游标分页</b>和 {@code <where>}/{@code <if>} 动态条件的使用.
     *
     * @param params 查询参数
     * @return 订单列表
     */
    List<Map<String, Object>> pageQuery(Map<String, Object> params);

    /**
     * 更新订单状态为已支付.
     *
     * @param orderInfo 订单信息
     * @return 影响行数
     */
    int updatePaid(OrderInfo orderInfo);

    /**
     * 更新订单状态为已发货.
     *
     * @param id 订单编号
     * @return 影响行数
     */
    int updateDelivered(@Param("id") String id);

    /**
     * 更新订单状态为已取消. 演示 {@code <set>}/{@code <if>} 进行选择性更新.
     *
     * @param orderInfo 订单信息
     * @return 影响行数
     */
    int updateCancelled(OrderInfo orderInfo);

    /**
     * 统计订单信息. 演示 {@code <choose>}/{@code <when>}/{@code <otherwise>} 动态查询的使用.
     *
     * @param params 查询参数
     * @return 统计结果
     */
    List<Map<String, Object>> statistics(Map<String, Object> params);
}
