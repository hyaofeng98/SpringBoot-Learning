package com.hyao.mybatis.controller;

import com.hyao.mybatis.model.order.*;
import com.hyao.mybatis.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单相关控制器.
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单.
     * <pre>
     *     请求示例:
     *     POST /order/create
     *
     *     Content-Type: application/json
     *     {
     *         "userId": "uid000000001",
     *         "receiverName": "hyao",
     *         "receiverPhone": "13800000000",
     *         "receiverAddress": "北京市海淀区中关村大街 1 号",
     *         "remark": "尽快发货啊",
     *         "items": [
     *             { "skuId": "SKU001", "quantity": 1 },
     *             { "skuId": "SKU002", "quantity": 2 }
     *         ]
     *     }
     * </pre>
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@Valid @RequestBody OrderCreateRequest req) {
        String orderId = orderService.createOrder(req);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        return result;
    }

    /**
     * 查询订单明细.
     * <pre>
     *     请求示例:
     *     POST /order/detail
     *
     *     Content-Type: application/json
     *     { "orderId": "900381547754946560" }
     * </pre>
     */
    @PostMapping("/detail")
    public OrderDetailVO queryDetail(@Valid @RequestBody OrderDetailRequest req) {
        return orderService.queryDetail(req);
    }

    /**
     * 分页查询订单.
     * <pre>
     *     请求示例:
     *     POST /order/page
     *
     *     Content-Type: application/json
     *     { "userId": "uid000000001", "createdFrom": "2026-10-11", "createdTo": "2026-10-31", "pageSize": 10 }
     * </pre>
     */
    @PostMapping("/page")
    public OrderPageVO pageQuery(@Valid @RequestBody OrderPageRequest req) {
        return orderService.orderPageQuery(req);
    }

    /**
     * 订单支付.
     * <pre>
     *     请求示例:
     *     POST /order/pay
     *
     *     Content-Type: application/json
     *     { "orderId": "900381547754946560", "payAmount": 657.00 }
     * </pre>
     */
    @PostMapping("/pay")
    public void payOrder(@Valid @RequestBody OrderPayRequest req) {
        orderService.payOrder(req);
    }

    /**
     * 订单发货.
     * <pre>
     *     请求示例:
     *     POST /order/deliver
     *
     *     Content-Type: application/json
     *     { "orderId": "900381547754946560" }
     * </pre>
     */
    @PostMapping("/deliver")
    public void deliverOrder(@Valid @RequestBody OrderDetailRequest req) {
        orderService.deliverOrder(req);
    }

    /**
     * 订单取消.
     * <pre>
     *     请求示例:
     *     POST /order/cancel
     *
     *     Content-Type: application/json
     *     {"orderId": "900381547754946560", "cancelledAt": "2026-10-20 21:11:21"}
     * </pre>
     */
    @PostMapping("/cancel")
    public void cancelOrder(@Valid @RequestBody OrderCancelRequest req) {
        orderService.cancelOrder(req);
    }

    /**
     * 订单统计.
     * <pre>
     *     请求示例:
     *     POST /order/statistics
     *
     *     Content-Type: application/json
     *     { "beginDate": "2027-08-03", "endDate": "2026-10-31", "dimension": "status" }
     * </pre>
     *
     * @param req 订单统计的请求
     * @return 订单统计结果
     */
    @PostMapping("/statistics")
    public List<OrderStatisticsItemVO> statistics(@Valid @RequestBody OrderStatisticsRequest req) {
        return orderService.statistics(req);
    }
}
