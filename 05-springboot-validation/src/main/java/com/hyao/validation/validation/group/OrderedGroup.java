package com.hyao.validation.validation.group;

import jakarta.validation.GroupSequence;

/**
 * 分组顺序校验的关键.
 * <ul>
 *     <li>{@code @GroupSequence}: 通过该注解定义校验分组的顺序.</li>
 * </ul>
 *
 * @see NotBlankGroup
 * @see ValidGroup
 */
@GroupSequence({NotBlankGroup.class, ValidGroup.class})
public interface OrderedGroup {
}

