package com.hyao.common.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyao.common.model.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应结果包装器。用于在 {@code @ResponseBody} 方法的返回值被 {@code HttpMessageConverter} 序列化写入响应体之前，对返回值做统一处理。
 * 它是实现统一响应包装（如 {@code Result<T}>）的标准方案。
 */
@ControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseWrapperAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 决定是否对当前这次响应应用包装逻辑。返回 {@code true} 才会进入 {@code beforeBodyWrite}方法。
     * 典型判断逻辑：
     * <ul>
     * <li>方法或类上是否标注了跳过注解</li>
     * <li>返回类型是否已经是 Result（避免重复包装）</li>
     * <li>是否由 Jackson 转换（排除 String、二进制流等）</li>
     * <li>是否属于业务包（排除 Swagger、Actuator）</li>
     * </ul>
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();
        // 如果方法上标注了跳过注解，则不进行包装
        if (returnType.hasMethodAnnotation(IgnoreWrapper.class)) {
            return false;
        }
        Class<?> containingClass = returnType.getContainingClass();
        if (containingClass.isAnnotationPresent(IgnoreWrapper.class)) {
            return false;
        }
        // 如果返回类型是 Result 类型，不重复包装
        if (Result.class.isAssignableFrom(parameterType)) {
            return false;
        }
        // 如果返回类型是 ResponseEntity 类型，不包装
        if (ResponseEntity.class.isAssignableFrom(parameterType)) {
            return false;
        }
        // 如果返回类型是 String，特殊包装
        if (String.class.isAssignableFrom(parameterType)) {
            return true;
        }
        // 只处理 Jackson 转换的 JSON 响应
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 真正改写返回值的地方。返回值将被 {@code HttpMessageConverter} 序列化写入响应体。
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }
        if (returnType.getParameterType() == String.class) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(Result.ok(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("响应包装序列化失败", e);
            }
        }
        return Result.ok(body);
    }
}
