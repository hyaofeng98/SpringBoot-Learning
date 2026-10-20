package com.hyao.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.id")
public class SnowflakeProperties {

    /** 机器 ID, 0~31 */
    private long workerId = 0L;

    /** 数据中心 ID, 0~31 */
    private long datacenterId = 0L;

}
