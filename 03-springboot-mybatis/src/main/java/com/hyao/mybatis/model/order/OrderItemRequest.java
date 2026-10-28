package com.hyao.mybatis.model.order;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 订单明细请求.
 * <p>商品价格以表中数据为准, 客户端传价格属于安全漏洞.
 */
@Data
public class OrderItemRequest {

    /** SKU编号 */
    @NotBlank(message = "SKU 编号不能为空")
    private String skuId;

    /** 商品数量. */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "至少为 1")
    @Max(value = 999, message = "最多为 999")
    private Integer quantity;
}
