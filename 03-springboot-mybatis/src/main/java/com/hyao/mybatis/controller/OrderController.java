package com.hyao.mybatis.controller;

import com.hyao.mybatis.model.*;
import com.hyao.mybatis.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
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

    @PostMapping("/create")
    public Map<String, Object> createOrder(@Valid @RequestBody OrderCreateRequest req) {
        String orderId = orderService.createOrder(req);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        return result;
    }

    @PostMapping("/detail")
    public OrderDetailVO queryDetail(@Valid @RequestBody OrderDetailRequest req) {
        return orderService.queryDetail(req);
    }

    @PostMapping("/page")
    public OrderPageVO pageQuery(@Valid @RequestBody OrderPageRequest req) {
        return orderService.orderPageQuery(req);
    }

    @PostMapping("/pay")
    public void payOrder(@Valid @RequestBody OrderPayRequest req) {
        orderService.payOrder(req);
    }

    @PostMapping("/deliver")
    public void deliverOrder(@Valid @RequestBody OrderDetailRequest req) {
        orderService.deliverOrder(req);
    }

    @PostMapping("/cancel")
    public void cancelOrder(@Valid @RequestBody OrderCancelRequest req) {
        orderService.cancelOrder(req);
    }
}
