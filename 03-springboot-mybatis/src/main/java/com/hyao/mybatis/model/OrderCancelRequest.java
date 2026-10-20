package com.hyao.mybatis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class OrderCancelRequest {

    @NotBlank(message = "订单编号不能为空")
    private String orderId;

    @NotNull(message = "取消时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelledAt;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
