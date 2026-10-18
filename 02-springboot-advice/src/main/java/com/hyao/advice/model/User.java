package com.hyao.advice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 用户信息。
 *
 * <p>这里的约束注解只有在 Controller 参数上标了 {@code @Valid} 时才会生效（见 {@code UserController#valid}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * 用户ID
     */
    private String id;

    /**
     * 用户名：必填，长度不超过 10
     */
    @NotBlank(message = "不能为空")
    @Size(max = 10, message = "长度不能超过 10")
    private String name;

    /**
     * 年龄：1 ~ 150
     */
    @Min(value = 1, message = "不能小于 1")
    @Max(value = 150, message = "不能大于 150")
    private Integer age;

    /**
     * 出生日期。不指定日期格式时按照默认格式解析。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
