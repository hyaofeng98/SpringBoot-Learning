package com.hyao.validation.model.dto.custom;

import com.hyao.validation.validation.annotation.PasswordMatch;
import com.hyao.validation.validation.annotation.Phone;
import com.hyao.validation.validation.annotation.UsernameUnique;
import com.hyao.validation.validation.annotation.ValidUsername;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 校验逻辑: 见自定义校验注解.
 *
 * @see ValidUsername
 * @see UsernameUnique
 * @see Phone
 * @see PasswordMatch
 */
@Data
@PasswordMatch(message = "两次输入的密码不一致")
public class CustomDTO {

    @ValidUsername
    @UsernameUnique
    private String username;

    @NotBlank(message = "手机号码不能为空")
    @Phone
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
