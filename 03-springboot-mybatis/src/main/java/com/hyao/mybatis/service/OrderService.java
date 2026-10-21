package com.hyao.mybatis.service;

import com.hyao.common.exception.BizException;
import com.hyao.mybatis.common.enums.OrderStatus;
import com.hyao.mybatis.common.enums.SkuStatus;
import com.hyao.mybatis.common.errorcode.BizErrorCode;
import com.hyao.mybatis.common.id.SnowflakeIdGenerator;
import com.hyao.mybatis.entity.order.OrderInfo;
import com.hyao.mybatis.entity.order.OrderItem;
import com.hyao.mybatis.entity.product.SkuInfo;
import com.hyao.mybatis.mapper.order.OrderInfoMapper;
import com.hyao.mybatis.mapper.order.OrderItemMapper;
import com.hyao.mybatis.mapper.product.SkuInfoMapper;
import com.hyao.mybatis.mapper.product.SkuStockMapper;
import com.hyao.mybatis.model.order.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务.
 */
@Service
public class OrderService {

    private final OrderInfoMapper orderInfoMapper;

    private final OrderItemMapper orderItemMapper;

    private final SkuInfoMapper skuInfoMapper;

    private final SkuStockMapper skuStockMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public OrderService(OrderInfoMapper orderInfoMapper,
                        OrderItemMapper orderItemMapper,
                        SkuInfoMapper skuInfoMapper,
                        SkuStockMapper skuStockMapper,
                        SnowflakeIdGenerator snowflakeIdGenerator) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.skuInfoMapper = skuInfoMapper;
        this.skuStockMapper = skuStockMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 创建订单.
     *
     * @param req 创建订单的请求
     * @return 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateRequest req) {
        // 1.查询商品并校验
        List<OrderItemRequest> orderItemRequests = req.getItems();
        List<String> skuIds = orderItemRequests.stream()
                .map(OrderItemRequest::getSkuId)
                .distinct()
                .collect(Collectors.toList());
        List<SkuInfo> skuInfos = skuInfoMapper.queryBySkuIds(skuIds);
        if (CollectionUtils.isEmpty(skuInfos)) {
            throw new BizException(BizErrorCode.SKU_NOT_FOUND, skuIds);
        }
        // skuId -> SkuInfo
        Map<String, SkuInfo> skuMap = skuInfos.stream().collect(Collectors.toMap(SkuInfo::getSkuId, skuInfo -> skuInfo));
        for (SkuInfo sku : skuMap.values()) {
            if (sku.getStatus() == SkuStatus.OFF_SHELF) {
                throw new BizException(BizErrorCode.SKU_OFF_SHELF, sku.getSkuId());
            }
        }
        // 2.计算金额
        BigDecimal totalAmount = orderItemRequests.stream().map(orderItemReq -> {
            BigDecimal price = skuMap.get(orderItemReq.getSkuId()).getPrice();
            Integer quantity = orderItemReq.getQuantity();
            return price.multiply(new BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 3.扣库存, 为了避免多SKU并发时交叉加锁导致死锁, 对SKU进行排序
        List<OrderItemRequest> sorted = req.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getSkuId))
                .collect(Collectors.toList());
        for (OrderItemRequest orderItemReq : sorted) {
            int affected = skuStockMapper.deduck(orderItemReq.getSkuId(), orderItemReq.getQuantity());
            if (affected == 0) {
                throw new BizException(BizErrorCode.SKU_STOCK_NOT_ENOUGH, orderItemReq.getSkuId());
            }
        }
        // 4.生成订单号并插入订单
        String orderNo = String.valueOf(snowflakeIdGenerator.nextId());
        OrderInfo orderInfo = buildOrderInfo(req, orderNo, totalAmount);
        orderInfoMapper.insert(orderInfo);
        // 5.批量插入订单明细
        List<OrderItem> orderItems = orderItemRequests.stream()
                .map(orderItemReq -> buildOrderItem(orderItemReq, skuMap, orderNo))
                .collect(Collectors.toList());
        orderItemMapper.batchInsert(orderItems);
        return orderNo;
    }

    /**
     * 根据订单编号查询订单详情.
     *
     * @param req 查询订单详情的请求
     * @return 订单详情
     */
    public OrderDetailVO queryDetail(OrderDetailRequest req) {
        String id = req.getOrderId();
        OrderInfo orderInfo = orderInfoMapper.queryDetail(id);
        if (orderInfo == null) {
            return null;
        }
        return buildOrderDetailVO(orderInfo);
    }

    /**
     * 按条件分页查询订单列表.
     *
     * @param req 查询订单列表的请求
     * @return 订单列表分页结果
     */
    public OrderPageVO orderPageQuery(OrderPageRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", req.getUserId());
        params.put("status", req.getStatus());
        params.put("createdFrom", req.getCreatedFrom());
        params.put("createdTo", req.getCreatedTo());
        params.put("minAmount", req.getMinAmount());
        params.put("maxAmount", req.getMaxAmount());
        params.put("limit", req.getPageSize() + 1);
        params.put("cursor", req.getCursor());
        List<Map<String, Object>> list = orderInfoMapper.pageQuery(params);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        OrderPageVO orderPageVO = new OrderPageVO();
        orderPageVO.setHasNextPage(list.size() > req.getPageSize());
        if (orderPageVO.isHasNextPage()) {
            orderPageVO.setNextCursor((String) (list.remove(list.size() - 1).get("id")));
        }
        orderPageVO.setRows(list);
        return orderPageVO;
    }

    /**
     * 订单支付.
     *
     * @param req 订单支付的请求
     */
    public void payOrder(OrderPayRequest req) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(req.getOrderId());
        orderInfo.setPayAmount(req.getPayAmount());
        int count = orderInfoMapper.updatePaid(orderInfo);
        if (count == 0) {
            throw new BizException(
                    BizErrorCode.ORDER_STATUS_UPDATE_FAILURE, OrderStatus.PENDING_PAYMENT.getDesc(), req.getOrderId());
        }
    }

    /**
     * 订单发货.
     *
     * @param req 订单明细的请求
     */
    public void deliverOrder(OrderDetailRequest req) {
        int count = orderInfoMapper.updateDelivered(req.getOrderId());
        if (count == 0) {
            throw new BizException(
                    BizErrorCode.ORDER_STATUS_UPDATE_FAILURE, OrderStatus.PAID.getDesc(), req.getOrderId());
        }
    }

    /**
     * 取消订单.
     *
     * @param req 取消订单的请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(OrderCancelRequest req) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(req.getOrderId());
        orderInfo.setRemark(req.getRemark());
        orderInfo.setCancelledAt(req.getCancelledAt());
        int count = orderInfoMapper.updateCancelled(orderInfo);
        if (count == 0) {
            throw new BizException(BizErrorCode.ORDER_CANCELLED_FAILURE, req.getOrderId());
        }
        // 取消订单后恢复库存
        List<OrderItem> orderItems = orderItemMapper.queryByOrderId(req.getOrderId());
        for (OrderItem orderItem : orderItems) {
            skuStockMapper.restore(orderItem.getSkuId(), orderItem.getQuantity());
        }
    }

    /**
     * 订单统计.
     *
     * @param req 订单统计的请求
     * @return 订单统计结果
     */
    public List<OrderStatisticsItemVO> statistics(OrderStatisticsRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("beginDate", req.getBeginDate());
        params.put("endDate", req.getEndDate());
        params.put("dimension", req.getDimension());
        params.put("status", req.getStatus());
        List<Map<String, Object>> rows = orderInfoMapper.statistics(params);
        List<OrderStatisticsItemVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            OrderStatisticsItemVO vo = new OrderStatisticsItemVO();
            Object statKey = row.get("stat_key");
            vo.setStatKey(statKey == null ? null : statKey.toString());
            vo.setOrderCount((Long) row.get("order_count"));
            vo.setTotalAmount(row.get("total_amount") == null ?
                    BigDecimal.ZERO : new BigDecimal(row.get("total_amount").toString()));
            if ("status".equals(req.getDimension())) {
                vo.setStatusDesc(OrderStatus.ofCode((Integer) row.get("stat_key")).getDesc());
            }
            result.add(vo);
        }
        return result;
    }

    private static OrderDetailVO buildOrderDetailVO(OrderInfo orderInfo) {
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setId(orderInfo.getId());
        orderDetailVO.setUserId(orderInfo.getUserId());
        orderDetailVO.setOrderStatus(orderInfo.getStatus());
        orderDetailVO.setTotalAmount(orderInfo.getTotalAmount());
        orderDetailVO.setPayAmount(orderInfo.getPayAmount());
        orderDetailVO.setReceiverName(orderInfo.getReceiverName());
        orderDetailVO.setReceiverPhone(orderInfo.getReceiverPhone());
        orderDetailVO.setReceiverAddress(orderInfo.getReceiverAddress());
        orderDetailVO.setRemark(orderInfo.getRemark());
        orderDetailVO.setCreatedAt(orderInfo.getCreatedAt());
        orderDetailVO.setUpdatedAt(orderInfo.getUpdatedAt());
        orderDetailVO.setCancelledAt(orderInfo.getCancelledAt());
        List<OrderItemVO> orderItemVOList = buildOrderItemVOList(orderInfo);
        orderDetailVO.setItems(orderItemVOList);
        return orderDetailVO;
    }

    private static List<OrderItemVO> buildOrderItemVOList(OrderInfo orderInfo) {
        List<OrderItem> items = orderInfo.getItems();
        List<OrderItemVO> orderItemVOList = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO orderItemVO = new OrderItemVO();
            orderItemVO.setId(item.getId());
            orderItemVO.setSkuId(item.getSkuId());
            orderItemVO.setSkuName(item.getSkuName());
            orderItemVO.setPrice(item.getPrice());
            orderItemVO.setQuantity(item.getQuantity());
            orderItemVO.setAmount(item.getAmount());
            orderItemVOList.add(orderItemVO);
        }
        return orderItemVOList;
    }

    private static OrderInfo buildOrderInfo(OrderCreateRequest req, String orderNo, BigDecimal totalAmount) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderNo);
        orderInfo.setUserId(req.getUserId());
        orderInfo.setStatus(OrderStatus.PENDING_PAYMENT);
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setPayAmount(BigDecimal.ZERO);
        orderInfo.setReceiverName(req.getReceiverName());
        orderInfo.setReceiverPhone(req.getReceiverPhone());
        orderInfo.setReceiverAddress(req.getReceiverAddress());
        orderInfo.setRemark(req.getRemark());
        return orderInfo;
    }

    private static OrderItem buildOrderItem(OrderItemRequest req, Map<String, SkuInfo> skuMap , String orderNo) {
        String skuId = req.getSkuId();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderNo);
        orderItem.setSkuId(skuId);
        orderItem.setSkuName(skuMap.get(skuId).getSkuName());
        orderItem.setPrice(skuMap.get(skuId).getPrice());
        orderItem.setQuantity(req.getQuantity());
        orderItem.setAmount(orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity())));
        return orderItem;
    }
}
