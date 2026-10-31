package com.hyao.validation.model.dto.assert0;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * {@code @AssertTrue}/{@code @AssertFalse} 的额外校验方式示例.
 * <p>通过在自定义校验方法加上这两个注解, 可以方便的实现跨字段校验. 但是需要注意的是:
 * <ul>
 *     <li>这类方法的命名必须符合 JavaBean getter 规范</li>
 *     <li>必须无参、返回 {@code boolean}/{@code Boolean}</li>
 * </ul>
 * 否则不会生效.
 */
@Data
public class AssertDTO {

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate begin;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate end;

    @AssertTrue(message = "密码不一致")
    @JsonIgnore
    private Boolean getPasswordMatch() {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return password.equals(confirmPassword);
    }

    @AssertTrue(message = "开始日期不能在结束日期之后")
    @JsonIgnore
    private boolean getDateValid() {
        if (begin == null || end == null) {
            return true;
        }
        return !begin.isAfter(end);
    }
}
