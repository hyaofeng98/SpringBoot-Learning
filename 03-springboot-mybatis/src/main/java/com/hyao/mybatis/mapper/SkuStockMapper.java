package com.hyao.mybatis.mapper;

import com.hyao.mybatis.entity.SkuStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品库存表({@code sku_stock}) Mapper. 演示注解 Mapper.
 */
public interface SkuStockMapper {

    /**
     * 根据 SKU 编号查询库存.
     *
     * @param skuId SKU 编号
     * @return 库存信息
     */
    @Select("SELECT sku_id, stock, version FROM `sku_stock` WHERE sku_id = #{skuId}")
    SkuStock queryBySkuId(@Param("skuId") String skuId);

    /**
     * 条件更新扣库存. (条件更新 防止并发超卖)
     *
     * @param skuId    SKU编号
     * @param quantity 扣减数量
     * @return 1 = 更新成功, 0 = 库存不足或 SKU 不存在.
     */
    @Update("UPDATE `sku_stock` SET stock = stock - #{quantity}, version = version + 1 WHERE sku_id = #{skuId} AND stock >= #{quantity}")
    int deduck(@Param("skuId") String skuId, @Param("quantity") int quantity);

    /**
     * 取消订单回补库存.
     *
     * @param skuId    SKU 编号
     * @param quantity 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE `sku_stock` SET stock = stock + #{quantity}, version = version + 1 WHERE sku_id = #{skuId}")
    int restore(@Param("skuId")String skuId, @Param("quantity")int quantity);
}
