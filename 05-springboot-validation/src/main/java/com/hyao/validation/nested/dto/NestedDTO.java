package com.hyao.validation.nested.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class NestedDTO {

    @Size(max = 4, message = "id 长度不能超过 4")
    private String id;
}
