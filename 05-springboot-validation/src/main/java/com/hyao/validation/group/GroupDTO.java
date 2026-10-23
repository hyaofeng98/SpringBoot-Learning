package com.hyao.validation.group;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
public class GroupDTO {

    @Null(groups = CreateGroup.class, message = "新增时 id 必须为空")
    @NotNull(groups = UpdateGroup.class, message = "更新时 id 不能为空")
    private Long id;

    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class}, message = "名称两个分组都不能为空")
    private String name;

    /**
     * 故意不加 groups
     */
    @NotBlank(message = "备注不能为空")
    private String remark;
}
