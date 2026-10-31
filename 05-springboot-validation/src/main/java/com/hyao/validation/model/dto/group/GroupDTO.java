package com.hyao.validation.model.dto.group;

import com.hyao.validation.validation.group.CreateGroup;
import com.hyao.validation.validation.group.UpdateGroup;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * 校验逻辑:
 * <ul>
 *     <li>走 {@code CreateGroup} 时, id 必须为空, 名称不能为空</li>
 *     <li>走 {@code UpdateGroup} 时, id 和名称必须都不为空</li>
 *     <li>走默认分组时, 备注不能为空</li>
 * </ul>
 * 实现方式: 通过注解中的 groups 属性指定分组, 不指定时就是默认分组.
 *
 * @see CreateGroup
 * @see UpdateGroup
 */
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
