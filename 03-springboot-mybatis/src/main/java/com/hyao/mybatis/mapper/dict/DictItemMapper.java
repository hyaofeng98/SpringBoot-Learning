package com.hyao.mybatis.mapper.dict;

import com.hyao.mybatis.entity.dict.DictItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据字典 Mapper 接口. 演示 Mybatis 中 {@code <cache>} 二级缓存的使用.
 */
public interface DictItemMapper {

    /**
     * 根据字典类型查询字典项. 演示开启二级缓存.
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    List<DictItem> queryByType(@Param("dictType") String dictType);

    /**
     * 根据字典类型、字典项编码更新字典项名称. 演示刷新缓存.
     *
     * @param dictType 字典类型
     * @param dictCode 字典项编码
     * @param dictName 字典项名称
     * @return 影响行数
     */
    int updateName(@Param("dictType") String dictType, @Param("dictCode") String dictCode, @Param("dictName") String dictName);

}
