package com.hyao.mybatis.entity;

import com.hyao.mybatis.common.enums.SkuStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息表(sku_info)实体类
 */
@Data
public class SkuInfo {
    /** SKU编号 */
    private String skuId;

    /** 商品名称 */
    private String skuName;

    /** 售价 */
    private BigDecimal price;

    /** 状态:1 上架/0 下架 */
    private SkuStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
