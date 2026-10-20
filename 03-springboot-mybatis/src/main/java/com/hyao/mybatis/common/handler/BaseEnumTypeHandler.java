package com.hyao.mybatis.common.handler;

import com.hyao.common.exception.BizException;
import com.hyao.mybatis.common.enums.BaseEnum;
import com.hyao.mybatis.common.errorcode.BizErrorCode;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis 类型处理器（{@code TypeHandler}）的泛型抽象基类.：让「实现了 {@code BaseEnum} 接口的枚举」与数据库中「整型」枚举字段之间
 * 自动完成双向转换。继承该抽象类的子类只需要写一个空壳子类即可(比如{@link GenderTypeHandler}).
 * <p>子类中需要加以下注解类:
 * <ul>
 *     <li>{@code @MappedTypes}: 指定该类型处理器处理的 Java 枚举类型.</li>
 *     <li>{@code @MappedJdbcTypes}: 指定该类型处理器处理的 JDBC 类型.</li>
 * </ul>
 * <p>通过在{@code application.yaml}中添加配置{@code mybatis.type-handlers-package}, 扫描注册类型处理器类所在的包, 才能生效.
 *
 * @param <E> 枚举类型
 */
public abstract class BaseEnumTypeHandler<E extends Enum<E> & BaseEnum> extends BaseTypeHandler<E> {

    private final Class<E> type;
    private final Map<Integer, E> codeMap;

    public BaseEnumTypeHandler(Class<E> type) {
        this.type = type;
        E[] enums = type.getEnumConstants();
        this.codeMap = new HashMap<>();
        for (E e : enums) {
            this.codeMap.put(e.getCode(), e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : codeOf(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : codeOf(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : codeOf(code);
    }

    private E codeOf(int code) {
        E e = codeMap.get(code);
        if (e == null) {
            throw new BizException(BizErrorCode.BIZ_ENUM_ERROR);
        }
        return e;
    }
}
