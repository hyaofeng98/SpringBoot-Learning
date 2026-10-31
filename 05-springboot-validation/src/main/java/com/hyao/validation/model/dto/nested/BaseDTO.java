package com.hyao.validation.model.dto.nested;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 用于演示嵌套对象或集合验证.
 */
@Data
public class BaseDTO {

    /**
     * 自定义类里面如果也有校验注解时, 需要使用 {@code @Valid} 注解才能生效.
     * <p>如果不加 {@code @Valid} 注解, 就只会校验 {@code nested} 这个成员变量是否为 {@code null},
     * 而不会校验其内部的 {@code id} 属性.
     */
    @Valid
    @NotNull
    private NestedDTO nested;

    /**
     * 自定义类集合的校验同上.
     */
    @Valid
    @NotEmpty
    private List<NestedDTO> list1;

    /**
     * 这是上一种自定义类集合校验的另一种方式. 将 {@code @Valid} 注解放在集合元素类型上.
     */
    @NotEmpty
    private List<@Valid NestedDTO> list2;

    /**
     * 还有集合中单字段校验的方式, 也是将要加的校验注解放在集合的类型上.
     */
    @NotEmpty
    private List<@NotBlank(message = "集合中的元素不能为空") String> list3;

    /**
     * 同上.
     */
    @NotEmpty
    private Map<@NotBlank(message = "键不能为空") String, @NotBlank(message = "值不能为空") String> map;
}
