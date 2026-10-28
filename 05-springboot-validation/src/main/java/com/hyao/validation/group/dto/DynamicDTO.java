package com.hyao.validation.group.dto;

import com.hyao.validation.group.groups.DynamicGroupProvider;
import com.hyao.validation.group.groups.OtherGroup;
import lombok.Data;
import org.hibernate.validator.group.GroupSequenceProvider;

import jakarta.validation.constraints.NotBlank;

/**
 * 校验逻辑: 类型不能为空, 类型为其他 {@code "OTHER"} 时, 说明字段不能为空.
 * <ul>
 *     <li>{@code @GroupSequenceProvider}: 当以 默认分组（Default） 触发校验时，不直接按默认规则走，
 *     而是先回调该注解指定类中的方法，由它返回一组本次实际要执行的校验分组序列。</li>
 * </ul>
 *
 * @see DynamicGroupProvider
 * @see OtherGroup
 */
@Data
@GroupSequenceProvider(DynamicGroupProvider.class)
public class DynamicDTO {

    @NotBlank(message = "类型不能为空")
    private String type;

    /**
     * 当 {@code OtherGroup} 被加入本次校验序列时才生效.
     *
     * @see DynamicGroupProvider#getValidationGroups(DynamicDTO)
     */
    @NotBlank(message = "说明不能为空", groups = OtherGroup.class)
    private String description;

}
