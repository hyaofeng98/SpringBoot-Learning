package com.hyao.advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 本模块的示例：演示公共库（00-springboot-common）里的三项能力。
 * <ul>
 *     <li>全局异常处理 {@link com.hyao.common.exception.GlobalExceptionHandler}</li>
 *     <li>响应结果包装 {@link com.hyao.common.advice.ResponseWrapperAdvice}</li>
 *     <li>统一日志 {@link com.hyao.common.log.TraceIdFilter}</li>
 * </ul>
 *
 *
 * 备注：这些 Bean 已经移到 00-springboot-common 公共库中。由 {@code com.hyao.common.autoconfigure.CommonWebAutoConfiguration}
 * 自动注册。
 *
 * @version 1.0 全局异常处理 + 响应结果包装 + 统一日志 2026/11/12 23:59
 * @version 2.0 对应配置类已放入公共库（00-springboot-common） 2026/11/13 19:00
 */
@SpringBootApplication
public class AdviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdviceApplication.class, args);
    }
}
