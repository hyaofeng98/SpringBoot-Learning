-- hyao.`user` 定义

CREATE TABLE `user` (
                        `id` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户id',
                        `nickname` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '昵称',
                        `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像url',
                        `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态, 1正常/2删除/0禁用',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deleted_at` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- hyao.user_profiles 定义

CREATE TABLE `user_profiles` (
                                 `user_id` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户id',
                                 `real_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
                                 `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别,0未知/1男/2女',
                                 `birthday` date DEFAULT NULL COMMENT '生日',
                                 `province` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省',
                                 `city` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '市',
                                 `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
                                 `company` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公司',
                                 `bio` text COLLATE utf8mb4_unicode_ci COMMENT '个人简介',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扩展资料表';