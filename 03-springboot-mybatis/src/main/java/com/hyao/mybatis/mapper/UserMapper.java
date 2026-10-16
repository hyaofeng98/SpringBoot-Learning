package com.hyao.mybatis.mapper;

import com.hyao.mybatis.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户Mapper接口。演示注解方式配置SQL。
 * <p>
 * 如果启动类不配置 {@code @MapperScan} 扫描，这里要单独添加 {@code @Mapper} 注解。
 */
public interface UserMapper {

    /**
     * 根据昵称查询用户
     *
     * @param nickname 用户昵称
     * @return 查询到的用户
     */
    @Select("SELECT * FROM user WHERE nickname=#{nickname}")
    User selectByNickname(@Param("nickname") String nickname);

    /**
     * 插入用户
     *
     * @param user 用户
     * @return 受影响行数
     */
    @Insert("INSERT INTO user(id, nickname, avatar, status) VALUES (#{id}, #{nickname}, #{avatar}, #{status})")
    int insert(User user);

    /**
     * 根据id更新昵称
     *
     * @param id       用户id
     * @param nickname 新昵称
     * @return 受影响行数
     */
    @Update("UPDATE user SET nickname=#{nickname} WHERE id=#{id}")
    int updateNicknameById(@Param("id") String id, @Param("nickname") String nickname);

    /**
     * 根据id删除用户. 逻辑删除.
     *
     * @param id 用户id
     * @return 受影响行数
     */
    @Delete("UPDATE user SET status=2, deleted_at=now() WHERE id=#{id} AND status!=2")
    int deleteById(@Param("id") String id);
}
