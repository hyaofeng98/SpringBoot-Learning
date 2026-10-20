package com.hyao.mybatis.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class OrderDetailRequest {

    @NotBlank
    @Size(max = 32, message = "长度不能超过 32")
    private String orderId;
}
