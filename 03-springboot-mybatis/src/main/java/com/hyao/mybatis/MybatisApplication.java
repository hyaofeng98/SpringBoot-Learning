package com.hyao.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mybatis + druid连接池配置使用示例。
 * 扫描Mapper的两种方式：
 * <ul>
 * <li>
 *     {@code @MapperScan} 启动类上统一扫描Mapper类，不用每个Mapper都加 {@code @Mapper} 注解（推荐）。
 * </li>
 * <li>
 *     {@code @Mapper} 在Mapper接口上逐个加注解。
 * </li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.hyao.mybatis.mapper")
public class MybatisApplication {
    public static void main(String[] args) {
        SpringApplication.run(MybatisApplication.class, args);
    }
}
