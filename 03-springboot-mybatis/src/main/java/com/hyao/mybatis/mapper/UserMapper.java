package com.hyao.mybatis.mapper;

import com.hyao.mybatis.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper 接口。演示 XML 方式配置 SQL。
 * <p>
 * 如果启动类不配置 {@code @MapperScan} 扫描，这里要单独添加 {@code @Mapper} 注解。
 */
public interface UserMapper {

    /**
     * 根据id查询用户.(演示{@code <sql>}和{@code <include>}标签的使用)
     *
     * @param id 用户id
     * @return 用户信息
     */
    User queryUserById(String id);

    /**
     * 按条件分页查询.(演示{@code <where>}搭配{@code <if>}标签的使用)
     *
     * @param params 查询参数
     * @return 用户列表
     */
    List<Map<String, Object>> pageQueryUserConditional(Map<String, Object> params);

    /**
     * 插入用户.
     *
     * @param user 用户列表
     * @return 受影响行数
     */
    int insertUser(User user);

    /**
     * 根据id更新用户信息.(演示{@code <set>}搭配{@code <if>}标签的使用)
     *
     * @param user 用户
     * @return 受影响行数
     */
    int updateUserSelective(User user);
}
