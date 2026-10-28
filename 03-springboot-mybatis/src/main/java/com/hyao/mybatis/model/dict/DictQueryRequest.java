package com.hyao.mybatis.model.dict;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典查询请求。
 */
@Data
public class DictQueryRequest {

    /** 字典类型 */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 64, message = "字典类型长度不能超过 64")
    private String dictType;
}
