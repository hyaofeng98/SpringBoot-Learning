package com.hyao.mybatis.model.order;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 订单创建请求.
 */
@Data
public class OrderCreateRequest {

    /** 用户ID */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 32)
    private String receiverName;

    /** 收货地址 */
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255)
    private String receiverAddress;

    /** 收货电话 */
    @NotBlank(message = "收货电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 订单项列表 */
    @NotEmpty(message = "至少一条订单明细")
    @Valid
    private List<OrderItemRequest> items;
}
