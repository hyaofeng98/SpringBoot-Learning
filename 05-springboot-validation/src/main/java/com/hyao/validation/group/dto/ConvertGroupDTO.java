package com.hyao.validation.group.dto;

import com.hyao.validation.group.groups.CreateGroup;
import com.hyao.validation.group.groups.OuterCreateGroup;
import com.hyao.validation.group.groups.OuterUpdateGroup;
import com.hyao.validation.group.groups.UpdateGroup;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;

/**
 * 级联分组转换.
 * <ul>
 *     <li>{@code @ConvertGroup}: 该注解将外层分组转换成内层分组, 用于解决嵌套对象默认沿用外层的分组的问题.</li>
 * </ul>
 *
 * @see OuterCreateGroup
 * @see GroupDTO
 */
@Data
public class ConvertGroupDTO {

    /**
     * 如果此处再加一个 {@code @ConvertGroup} , 级联分组转换会失效.
     */
    @Valid
    @ConvertGroup(from = OuterCreateGroup.class, to = CreateGroup.class)
    @ConvertGroup(from = OuterUpdateGroup.class, to = UpdateGroup.class)
    private GroupDTO dto;

}
