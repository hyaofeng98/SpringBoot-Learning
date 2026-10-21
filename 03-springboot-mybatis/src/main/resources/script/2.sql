CREATE TABLE IF NOT EXISTS `dict_item` (
    `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dict_type`  varchar(64)  NOT NULL COMMENT '字典类型，如 order_status',
    `dict_code`  varchar(64)  NOT NULL COMMENT '字典编码',
    `dict_name`  varchar(128) NOT NULL COMMENT '显示名',
    `sort`       int          NOT NULL DEFAULT '0' COMMENT '排序（升序）',
    `status`     tinyint      NOT NULL DEFAULT '1' COMMENT '状态: 1 启用 / 0 停用',
    `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`),
    KEY `idx_type_sort` (`dict_type`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据字典';

INSERT IGNORE INTO `dict_item` (`dict_type`, `dict_code`, `dict_name`, `sort`) VALUES
('order_status', '0', '待支付', 1),
('order_status', '1', '已支付', 2),
('order_status', '2', '已发货', 3),
('order_status', '3', '已完成', 4),
('order_status', '4', '已取消', 5),
('sku_status', '0', '下架', 1),
('sku_status', '1', '上架', 2);