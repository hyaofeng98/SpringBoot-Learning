package com.hyao.mybatis.mapper;

import com.hyao.mybatis.entity.SkuInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品信息表({@code sku_info}) Mapper. 演示注解动态 SQL.
 */
public interface SkuInfoMapper {

    @Select({"<script>",
            "SELECT sku_id, sku_name, price, status FROM `sku_info` WHERE sku_id IN",
            "<foreach collection='skuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<SkuInfo> queryBySkuIds(@Param("skuIds") List<String> skuIds);


}
