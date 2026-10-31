package com.hyao.validation.model.dto.group;

import com.hyao.validation.validation.group.CreateGroup;
import com.hyao.validation.validation.group.OuterCreateGroup;
import com.hyao.validation.validation.group.OuterUpdateGroup;
import com.hyao.validation.validation.group.UpdateGroup;
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
     * 注意: {@code @ConvertGroup} 是 {@code @Repeatable} 的, 写两个及以上会被编译器合并成
     * {@code @ConvertGroup.List}, Spring Boot 早期版本中 Hibernate Validator 无法识别，会导致级联分组转换静默失效;
     * 从 3.3 版本开始重复的 {@code @ConvertGroup} 注解已能正确处理。
     */
    @Valid
    @ConvertGroup(from = OuterCreateGroup.class, to = CreateGroup.class)
    @ConvertGroup(from = OuterUpdateGroup.class, to = UpdateGroup.class)
    private GroupDTO dto;

}
