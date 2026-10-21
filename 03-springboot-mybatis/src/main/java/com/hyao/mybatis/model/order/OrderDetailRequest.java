package com.hyao.mybatis.model.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 订单详情请求.
 */
@Data
public class OrderDetailRequest {

    /** 订单编号 */
    @NotBlank
    @Size(max = 32, message = "长度不能超过 32")
    private String orderId;
}
