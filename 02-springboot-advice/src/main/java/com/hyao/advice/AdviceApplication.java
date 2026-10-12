package com.hyao.advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 本模块中的示例：
 * <ul>
 *     <li>全局异常处理 {@link com.hyao.advice.exception.GlobalExceptionHandler}</li>
 *     <li>响应结果包装 {@link com.hyao.advice.advice.ResponseWrapperAdvice}</li>
 * </ul>
 */
@SpringBootApplication
public class AdviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdviceApplication.class, args);
    }
}
