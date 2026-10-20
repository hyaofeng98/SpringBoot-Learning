package com.hyao.mybatis.config;

import com.hyao.mybatis.common.id.SnowflakeIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类.
 * <ul>
 *     <li>{@code @Configuration}: 配置注解, 表示这是一个配置类</li>
 *     <li>{@code @EnableConfigurationProperties}: 启用配置属性, 表示启用配置属性支持</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfig {

    private final SnowflakeProperties snowflakeProperties;

    public SnowflakeConfig(SnowflakeProperties snowflakeProperties) {
        this.snowflakeProperties = snowflakeProperties;
    }

    /**
     * 雪花算法ID生成器.
     * <ul>
     *     <li>{@code @Bean}: Bean 注解把该方法返回的实例放入容器中.</li>
     * </ul>
     *
     * @return 雪花算法ID生成器
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(
                snowflakeProperties.getDatacenterId(),
                snowflakeProperties.getWorkerId()
        );
    }
}
