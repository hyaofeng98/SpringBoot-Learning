package com.hyao.mybatis.common.enums;

/**
 * 数据库枚举接口. 约定枚举必须提供一个{@code getCode()}方法，返回{@code int}类型(即数据库里存的数字).
 */
public interface BaseEnum  {
    int getCode();
}
