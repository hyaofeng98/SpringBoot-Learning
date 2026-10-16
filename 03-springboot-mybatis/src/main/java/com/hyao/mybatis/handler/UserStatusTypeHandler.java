package com.hyao.mybatis.handler;

import com.hyao.mybatis.common.enums.UserStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 用户状态类型处理器.
 */
@MappedTypes(UserStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class UserStatusTypeHandler extends BaseEnumTypeHandler<UserStatus> {

    public UserStatusTypeHandler() {
        super(UserStatus.class);
    }
}
