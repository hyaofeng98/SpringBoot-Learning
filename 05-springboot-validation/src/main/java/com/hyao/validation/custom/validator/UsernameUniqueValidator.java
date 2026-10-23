package com.hyao.validation.custom.validator;

import com.hyao.validation.custom.annotation.UsernameUnique;
import com.hyao.validation.custom.mapper.UserRepository;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 用户名唯一的校验器.
 * <p>{@code ConstraintValidator} 校验器中可以注入 Spring Bean.
 * <b>注意:</b> 校验器上不能加 {@code @Component}, 且依赖通过构造器或字段注入.
 */
public class UsernameUniqueValidator implements ConstraintValidator<UsernameUnique, String> {

    private final UserRepository userRepository;

    public UsernameUniqueValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // 由于UsernameUnique注解是放在成员变量上的, 所以不用像PasswordMatchValidator那样手动构建错误.
        return userRepository.selectCount(value) == 0;
    }
}
