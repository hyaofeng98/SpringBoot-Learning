package com.hyao.validation.custom.annotation;

import com.hyao.validation.custom.validator.PasswordMatchValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 两次输入密码是否一致校验注解. 跨字段的类级别注解示例.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {

    String message() default "两次密码输入不一致";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
