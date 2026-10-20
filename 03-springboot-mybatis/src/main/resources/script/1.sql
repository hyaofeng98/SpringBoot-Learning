-- 商品信息表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sku_info` (
    `sku_id`     varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU 编号',
    `sku_name`   varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
    `price`      decimal(12, 2) NOT NULL DEFAULT '0.00' COMMENT '售价',
    `status`     tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1 上架 / 0 下架',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`sku_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商品信息表';

-- -----------------------------------------------------------------------------
-- 商品库存表（与 sku_info 一对一，拆开是为了扣减时不锁商品主表）
-- 扣减 SQL：UPDATE `sku_stock` SET stock = stock - #{quantity}, version = version + 1
--           WHERE sku_id = #{skuId} AND version = #{version} AND stock >= #{quantity}
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sku_stock` (
    `sku_id`     varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU 编号',
    `stock`      int NOT NULL DEFAULT '0' COMMENT '可售库存',
    `version`    int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号（每次扣减 +1）',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`sku_id`),
    KEY `idx_stock` (`stock`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商品库存表';

-- -----------------------------------------------------------------------------
-- 订单主表
-- id 是字符串主键（应用生成），同时就是订单号；不用自增，演示主键 ID 生成。
-- 注意：要做 keyset 游标分页，id 必须「定长 + 单调递增」（雪花 / 时间戳序列都满足）。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_info` (
    `id`               varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号（字符串主键，应用生成）',
    `user_id`          varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '下单用户id',
    `status`           tinyint NOT NULL DEFAULT '0' COMMENT '订单状态: 0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消',
    `total_amount`     decimal(12, 2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
    `pay_amount`       decimal(12, 2) NOT NULL DEFAULT '0.00' COMMENT '实付金额',
    `receiver_name`    varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人',
    `receiver_phone`   varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人电话',
    `receiver_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货地址',
    `remark`           varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单备注',
    `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `cancelled_at`     datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '取消时间（未取消为 1970-01-01）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `id`),
    KEY `idx_status_id` (`status`, `id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='订单主表';

-- -----------------------------------------------------------------------------
-- 订单明细表
-- sku_name / price 是「下单时的快照」；order_id 关联 order_info.id（字符串），不建外键。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '明细id（自增，单条插入可回填）',
    `order_id`   varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号（order_info.id）',
    `sku_id`     varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKU 编号',
    `sku_name`   varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称快照',
    `price`      decimal(12, 2) NOT NULL DEFAULT '0.00' COMMENT '单价快照',
    `quantity`   int NOT NULL DEFAULT '1' COMMENT '购买数量',
    `amount`     decimal(12, 2) NOT NULL DEFAULT '0.00' COMMENT '小计金额（price * quantity）',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='订单明细表';

-- =============================================================================
-- 初始化数据（可重复执行）
-- =============================================================================

INSERT IGNORE INTO `sku_info` (`sku_id`, `sku_name`, `price`, `status`)
VALUES ('SKU001', '机械键盘', 399.00, 1),
       ('SKU002', '无线鼠标', 129.00, 1),
       ('SKU003', '27寸显示器', 1299.00, 1),
       ('SKU004', '已下架商品', 99.00, 0);

INSERT IGNORE INTO `sku_stock` (`sku_id`, `stock`, `version`)
VALUES ('SKU001', 100, 0),
       ('SKU002', 200, 0),
       ('SKU003', 50, 0),
       ('SKU004', 0, 0);

-- 两条示例订单：一条已支付（验证详情/列表），一条待支付（验证状态筛选）
-- 示例 id 格式：'ORD' + yyyyMMddHHmmss + 序列，定长 17，字典序 = 时间序
INSERT IGNORE INTO `order_info` (`id`, `user_id`, `status`, `total_amount`, `pay_amount`,
                                 `receiver_name`, `receiver_phone`, `receiver_address`, `remark`)
VALUES ('ORD20261019000001', 'uid000000001', 1, 528.00, 528.00,
        'hyao', '13800000000', '北京市海淀区中关村大街 1 号', '示例订单：已支付'),
       ('ORD20261019000002', 'uid000000002', 0, 1299.00, 1299.00,
        '星野旅行者', '13900000000', '上海市浦东新区世纪大道 100 号', '示例订单：待支付');

INSERT IGNORE INTO `order_item` (`order_id`, `sku_id`, `sku_name`, `price`, `quantity`, `amount`)
VALUES ('ORD20261019000001', 'SKU001', '机械键盘', 399.00, 1, 399.00),
       ('ORD20261019000001', 'SKU002', '无线鼠标', 129.00, 1, 129.00),
       ('ORD20261019000002', 'SKU003', '27寸显示器', 1299.00, 1, 1299.00);
