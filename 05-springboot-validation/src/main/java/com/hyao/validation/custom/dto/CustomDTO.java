package com.hyao.validation.custom.dto;

import com.hyao.validation.custom.annotation.PasswordMatch;
import com.hyao.validation.custom.annotation.Phone;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@PasswordMatch(message = "两次输入的密码不一致")
public class CustomDTO {

    @NotBlank(message = "手机号码不能为空")
    @Phone
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
