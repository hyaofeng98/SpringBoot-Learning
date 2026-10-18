package com.hyao.mybatis.entity;

import com.hyao.mybatis.common.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 {@code user} 对应的实体类。
 *
 * <p>表结构见 {@code src/main/resources/script/1.sql}。
 */
@Data
public class User {

    /** 用户ID */
    private String id;

    /** 昵称 */
    private String nickname;

    /** 头像url */
    private String avatar;

    /** 状态, 1正常/2删除/0禁用 */
    private UserStatus status;

    /** 创建时间（create_at，默认 CURRENT_TIMESTAMP） */
    private LocalDateTime createdAt;

    /** 更新时间（update_at，默认 CURRENT_TIMESTAMP ON UPDATE） */
    private LocalDateTime updatedAt;

    /** 删除时间（delete_at） */
    private LocalDateTime deletedAt;
}
