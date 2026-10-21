package com.hyao.mybatis.model.dict;

import lombok.Data;

@Data
public class DictItemVO {

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 显示名 */
    private String dictName;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private Integer status;

}
