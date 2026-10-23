package com.hyao.validation.custom.validator;

import com.hyao.validation.custom.annotation.PasswordMatch;
import com.hyao.validation.custom.dto.CustomDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 密码匹配注解的校验器.
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
        return password.equals(confirmPassword);
    }
}
