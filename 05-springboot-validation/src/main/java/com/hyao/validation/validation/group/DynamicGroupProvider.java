package com.hyao.validation.validation.group;

import com.hyao.validation.model.dto.group.DynamicDTO;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态分组校验的核心实现.
 * <p><b>注意:</b> Hibernate Validator 硬性要求必须把 DTO 自身加进去, 代表 Default 分组.
 */
public class DynamicGroupProvider implements DefaultGroupSequenceProvider<DynamicDTO> {
    @Override
    public List<Class<?>> getValidationGroups(DynamicDTO dto) {
        List<Class<?>> groups = new ArrayList<>();
        groups.add(DynamicDTO.class);
        if (dto != null && "OTHER".equals(dto.getType())) {
            // 当类型为 OTHER 时, 追加 OtherGroup 分组
            groups.add(OtherGroup.class);
        }
        return groups;
    }
}
