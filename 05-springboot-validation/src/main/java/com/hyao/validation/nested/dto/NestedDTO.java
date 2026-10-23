package com.hyao.validation.nested.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class NestedDTO {

    @Size(max = 4, message = "长度不能超过 4")
    private String id;
}
