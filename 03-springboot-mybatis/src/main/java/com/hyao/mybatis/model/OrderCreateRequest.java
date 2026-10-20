package com.hyao.mybatis.model;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 订单创建请求.
 */
@Data
public class OrderCreateRequest {

    /** 用户ID */
    @NotBlank(message = "不能为空")
    private String userId;

    /** 收货人姓名 */
    @NotBlank(message = "不能为空")
    @Size(max = 32)
    private String receiverName;

    /** 收货地址 */
    @NotBlank(message = "不能为空")
    @Size(max = 255)
    private String receiverAddress;

    /** 收货电话 */
    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;

    /** 备注 */
    @Size(max = 255)
    private String remark;

    /** 订单项列表 */
    @NotEmpty(message = "至少一条订单明细")
    @Valid
    private List<OrderItemRequest> items;
}
