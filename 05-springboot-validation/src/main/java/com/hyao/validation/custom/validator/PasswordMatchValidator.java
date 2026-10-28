package com.hyao.validation.custom.validator;

import com.hyao.validation.custom.annotation.PasswordMatch;
import com.hyao.validation.custom.dto.CustomDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码匹配注解的校验器.
 * <p>由于 {@code @PasswordMatch} 是类级别注解, 校验失败的错误信息也是类级别的, 因此如果想要将错误精确地挂在 {@code confirmPassword}
 * 属性上, 可以手动构建错误信息.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, CustomDTO> {
    @Override
    public boolean isValid(CustomDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();
        if (password == null || confirmPassword == null) {
            return true;
        }
        if (password.equals(confirmPassword)) {
            return true;
        }
        // 手动构建错误
        // 关闭默认的类级别错误, 不关闭的话会出现两条报错信息
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
