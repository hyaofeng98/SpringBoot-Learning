package com.hyao.mybatis.common.handler;

import com.hyao.mybatis.common.enums.SkuStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 商品状态类型处理器.
 *
 * @see com.hyao.mybatis.common.handler.BaseEnumTypeHandler
 */
@MappedTypes(SkuStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class SkuStatusTypeHandler extends BaseEnumTypeHandler<SkuStatus>{

    public SkuStatusTypeHandler(Class<SkuStatus> type) {
        super(type);
    }
}
