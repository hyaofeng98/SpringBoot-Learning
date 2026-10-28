package com.hyao.mybatis.model.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 订单取消请求.
 */
@Data
public class OrderCancelRequest {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderId;

    /** 取消时间 */
    @NotNull(message = "取消时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelledAt;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
