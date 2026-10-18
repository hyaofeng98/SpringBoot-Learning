package com.hyao.mybatis.mapper;

import com.hyao.mybatis.entity.UserProfiles;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户扩展信息 Mapper 接口。演示注解方式配置 SQL 。
 */
public interface UserProfilesMapper {
    /**
     * 插入用户扩展信息.
     *
     * @param userProfiles 用户扩展信息
     * @return 受影响行数
     */
    @Insert("INSERT INTO user_profiles(user_id, real_name, gender, birthday, province, city, address, company, bio) " +
            "VALUES (#{userId}, #{realName}, #{gender}, #{birthday}, #{province}, #{city}, #{address}, #{company}, #{bio})")
    int insertUserProfiles(UserProfiles userProfiles);

    /**
     * 根据用户ID查询用户扩展信息.
     *
     * @param userId 用户ID
     * @return 用户扩展信息
     */
    @Select("SELECT * FROM user_profiles WHERE user_id=#{userId}")
    UserProfiles queryUserProfileByUserId(@Param("userId")String userId);

    /**
     * 根据用户ID更新用户个人简介.
     *
     * @param userId 用户ID
     * @param bio 用户个人简介
     * @return 受影响行数
     */
    @Update("UPDATE user_profiles SET bio = #{bio} WHERE user_id = #{userId}")
    int updateUserBio(@Param("userId") String userId, @Param("bio") String bio);

    /**
     * 根据用户ID更新用户扩展信息.
     * <p>注意：这种更新之前需要查一次数据库，获取完整数据再更新，不然没更新的字段会被更新为NULL</p>
     *
     * @param userProfiles 用户扩展信息
     * @return 受影响行数
     */
    @Update("UPDATE user_profiles SET real_name=#{realName}, gender=#{gender}, birthday=#{birthday}, province=#{province}, " +
            "city=#{city}, address=#{address}, company=#{company}, bio=#{bio} WHERE user_id=#{userId}")
    int updateUserProfiles(UserProfiles userProfiles);

}
