package com.hyao.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 雪花算法配置值.
 * <ul>
 *     <li>{@code @ConfigurationProperties}: 配置属性注解, 用于将 {@code application.yaml} 中对应前缀的配置绑定到该类.</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "app.id")
public class SnowflakeProperties {

    /** 机器 ID, 0~31 */
    private long workerId = 0L;

    /** 数据中心 ID, 0~31 */
    private long datacenterId = 0L;

}
