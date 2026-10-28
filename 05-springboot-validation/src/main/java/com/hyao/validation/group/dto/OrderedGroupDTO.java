package com.hyao.validation.group.dto;

import com.hyao.validation.group.groups.NotBlankGroup;
import com.hyao.validation.group.groups.ValidGroup;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用于验证分组顺序校验.
 */
@Data
public class OrderedGroupDTO {

    @NotBlank(message = "ID不能为空", groups = NotBlankGroup.class)
    private String id;

    @Size(max = 6, message = "名称长度不能超过 6", groups = ValidGroup.class)
    private String name;

    @NotBlank(message = "地址不能为空", groups = NotBlankGroup.class)
    private String address;

    @Size(max = 11, message = "手机号长度不能超过 11", groups = ValidGroup.class)
    private String phone;

}
