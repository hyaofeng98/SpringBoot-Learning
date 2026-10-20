package com.hyao.mybatis.config;

import com.hyao.mybatis.common.id.SnowflakeIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfig {

    private final SnowflakeProperties snowflakeProperties;

    public SnowflakeConfig(SnowflakeProperties snowflakeProperties) {
        this.snowflakeProperties = snowflakeProperties;
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(
                snowflakeProperties.getDatacenterId(),
                snowflakeProperties.getWorkerId()
        );
    }
}
