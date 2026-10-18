package com.hyao.mybatis.entity;

import com.hyao.mybatis.common.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户扩展信息表 {@code user_profiles} 对应的实体类。
 *
 * <p>表结构见 {@code src/main/resources/script/1.sql}。
 */
@Data
public class UserProfiles {

    /** 用户ID（varchar(16) 主键 */
    private String userId;

    /** 真实姓名 */
    private String realName;

    /** 性别（tinyint, 0-未知/1-男/2-女） */
    private Gender gender;

    /** 生日（date） */
    private LocalDate birthday;

    /** 省份（varchar(32)） */
    private String province;

    /** 城市（varchar(32)） */
    private String city;

    /** 地址（varchar(255)） */
    private String address;

    /** 公司（varchar(128)） */
    private String company;

    /** 个人简介（text） */
    private String bio;

    /** 创建时间（create_at，默认 CURRENT_TIMESTAMP） */
    private LocalDateTime createdAt;

    /** 更新时间（update_at，默认 CURRENT_TIMESTAMP ON UPDATE） */
    private LocalDateTime updatedAt;
}
