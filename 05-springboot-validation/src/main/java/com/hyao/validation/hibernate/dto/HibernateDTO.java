package com.hyao.validation.hibernate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.*;

import java.util.Date;

/**
 * Hibernate Validator 注解示例.
 *
 * {@code @ScriptAssert} 注解不推荐使用了，推荐用标准注解或自定义校验器来替代。
 */
@Data
@ScriptAssert(lang = "javascript",
        script = "_this.date == null || _this.date.time >= Date.now()",
        message = "日期必须大于当前时间",
        reportOn = "date")
public class HibernateDTO {

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date date;

    /**
     * 类似于 {@code @Size} 注解，但语义更明确.
     */
    @Length(min = 1, max = 10, message = "长度必须在 1 到 10 之间")
    private String l;

    /**
     * 类似于{@code @Min} 和 {@code @Max} 注解合并使用.
     */
    @Range(min = 0, max = 10, message = "数值必须在 1 到 10 之间")
    private Integer r;

    /**
     * URL格式校验. 底层是基于正则的.
     */
    @URL(protocol = "https", host = "example.com", message = "URL 必须是 https 协议，主机是 example.com")
    private String url;

    /**
     * 通过 Luhn 算法验证. 这种算法用来验证各种标识号码（如信用卡号、银行卡号、手机 IMEI 码等）是否合法。
     * <p>这个注解能提前过滤掉随便敲的一串数字.
     */
    @CreditCardNumber(message = "信用卡号必须符合格式")
    private String creditCard;

    /**
     * ISBN 书号.
     */
    @ISBN(type = ISBN.Type.ANY)
    private String isbn;
}
