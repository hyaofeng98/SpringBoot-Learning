package com.hyao.validation.model.dto.hibernate;

import lombok.Data;
import org.hibernate.validator.constraints.*;

/**
 * Hibernate Validator 注解示例.
 * <p>{@code org.hibernate.validator.constraints.ScriptAssert} 在 HV 8.0.3 中仍然存在, 但它依赖 JSR-223 脚本引擎,
 * 而 JDK 15+ 已移除 Nashorn, 实际无法使用; 类内断言请改用 {@code @AssertTrue}/{@code @AssertFalse}（见 {@code AssertDTO}）。
 */
@Data
public class HibernateDTO {

    /**
     * 类似于 {@code @Size} 注解，但语义更明确.
     */
    @Length(min = 1, max = 10, message = "l 长度必须在 1 到 10 之间")
    private String l;

    /**
     * 类似于{@code @Min} 和 {@code @Max} 注解合并使用.
     */
    @Range(min = 0, max = 10, message = "r 数值必须在 1 到 10 之间")
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
    @ISBN(type = ISBN.Type.ANY, message = "ISBN 书号必须符合格式")
    private String isbn;
}
