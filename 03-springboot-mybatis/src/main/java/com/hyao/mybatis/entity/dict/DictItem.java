package com.hyao.mybatis.entity.dict;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictItem {
    /** 主键 id */
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 显示名 */
    private String dictName;

    /** 排序, 升序 */
    private Integer sort;

    /** 状态, 0停用/1启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
