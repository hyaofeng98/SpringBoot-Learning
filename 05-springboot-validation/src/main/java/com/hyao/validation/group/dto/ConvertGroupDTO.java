package com.hyao.validation.group.dto;

import com.hyao.validation.group.groups.CreateGroup;
import com.hyao.validation.group.groups.OuterCreateGroup;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;

/**
 * 级联分组转换.
 * <ul>
 *     <li>{@code @ConvertGroup}: 该注解将外层分组转换成内层分组, 用于解决嵌套对象默认沿用外层的分组的问题.</li>
 * </ul>
 * <b>注意:</b> {@code @ConvertGroup} 在 Bean Validation 2.0 里是 {@code @Repeatable} 的, javac 会把多个
 * {@code @ConvertGroup} 合成一个 {@code @ConvertGroup.List} 容器注解, 但是 Hibernate Validator 6.2.5
 * （Boot 2.7.18 自带）只识别直接标注在字段上的 {@code @ConvertGroup}，{@code @ConvertGroup.List} 被忽略.
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
    private GroupDTO dto;

}
