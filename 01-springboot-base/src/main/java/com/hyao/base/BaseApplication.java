package com.hyao.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot 项目的启动类。
 * <ul>
 *     <li>{@code @SpringBootApplication}: 启动类注解</li>
 * </ul>
 */
@SpringBootApplication
public class BaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaseApplication.class, args);
    }
}
