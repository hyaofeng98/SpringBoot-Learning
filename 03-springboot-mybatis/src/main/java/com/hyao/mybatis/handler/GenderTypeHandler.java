package com.hyao.mybatis.handler;

import com.hyao.mybatis.common.enums.Gender;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 性别类型处理器.
 */
@MappedTypes(Gender.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class GenderTypeHandler extends BaseEnumTypeHandler<Gender> {

    public GenderTypeHandler() {
        super(Gender.class);
    }
}
