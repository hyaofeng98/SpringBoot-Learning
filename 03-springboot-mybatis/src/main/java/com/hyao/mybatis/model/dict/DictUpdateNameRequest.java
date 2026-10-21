package com.hyao.mybatis.model.dict;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新字典编码显示名请求。
 */
@Data
public class DictUpdateNameRequest {

    /** 字典类型 */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 64, message = "字典类型长度不能超过 64")
    private String dictType;

    /** 字典编码 */
    @NotBlank(message = "字典编码不能为空")
    @Size(max = 64, message = "字典编码长度不能超过 64")
    private String dictCode;

    /** 显示名 */
    @NotBlank(message = "显示名不能为空")
    @Size(max = 128, message = "显示名长度不能超过 128")
    private String dictName;
}
