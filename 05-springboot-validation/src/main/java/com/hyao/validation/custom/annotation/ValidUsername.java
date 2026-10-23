package com.hyao.validation.custom.annotation;

import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.constraints.ConstraintComposition;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 复合校验注解.
 * <ul>
 *     <li>{@code @ReportAsSingleViolation}: 将多个校验错误信息合并为一个, 不逐条报错. 这样的效果是校验不通过的信息就是
 *     {@code @ValidUsername} 中 message 的内容了.</li>
 *     <li>{@code @ConstraintComposition}: 复合约束的关键, 组合多个校验注解, 作为复合校验注解使用. 这里使用
 *     {@code CompositionType.AND} 表示所有校验注解都必须同时满足.</li>
 * </ul>
 */
@NotBlank
@Size(min = 2, max = 10)
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
@ReportAsSingleViolation
@ConstraintComposition(CompositionType.AND)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ValidUsername {
    String message() default "用户名只能包含字母数字下划线，且长度限定 2~10 位";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
