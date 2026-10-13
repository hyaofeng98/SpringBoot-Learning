package com.hyao.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyao.common.advice.ResponseWrapperAdvice;
import com.hyao.common.exception.GlobalExceptionHandler;
import com.hyao.common.log.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 公共能力的自动配置。
 *
 * <p>只要 classpath 上有本模块的 jar，Spring Boot 就会通过
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 找到这个类，并把下面几个 Bean 注册进容器——使用方不需要写 {@code @ComponentScan} /
 * {@code scanBasePackages}，也不会因为忘写扫描包而静默失效。
 *
 * <p>三个 Bean 都加了 {@code @ConditionalOnMissingBean}：使用方自己定义了同类型 Bean 时，以使用方为准。
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ResponseBodyAdvice.class, MappingJackson2HttpMessageConverter.class, ObjectMapper.class})
public class CommonWebAutoConfiguration {

    /**
     * 统一响应包装：需要用到 Spring Boot 自动配置的那个 ObjectMapper，所以本配置类排在 JacksonAutoConfiguration 之后。
     */
    @Bean
    @ConditionalOnMissingBean
    public ResponseWrapperAdvice responseWrapperAdvice(ObjectMapper objectMapper) {
        return new ResponseWrapperAdvice(objectMapper);
    }

    /**
     * 全局异常处理：业务异常返回 200 + 业务码，框架异常保留原始 HTTP 状态码
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * traceId 过滤器：类上有 @Order(HIGHEST_PRECEDENCE)，注册后排在过滤器链最前面
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }
}
