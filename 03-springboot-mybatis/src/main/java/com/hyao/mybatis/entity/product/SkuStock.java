package com.hyao.mybatis.entity.product;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品库存表(sku_stock)实体类.
 */
@Data
public class SkuStock {
    /** SKU编号 */
    private String skuId;

    /** 可售库存  */
    private Integer stock;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
