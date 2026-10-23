package com.hyao.validation.group.groups;

import javax.validation.GroupSequence;

/**
 * 分组顺序校验的关键.
 * <ul>
 *     <li>{@code @GroupSequence}: 通过该注解定义校验分组的顺序.</li>
 * </ul>
 *
 * @see com.hyao.validation.group.groups.NotBlankGroup
 * @see com.hyao.validation.group.groups.ValidGroup
 */
@GroupSequence({NotBlankGroup.class, ValidGroup.class})
public interface OrderedGroup {
}

