package com.hyao.validation.basic.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 基本注解的使用示例. 注意:
 * <ul>
 *     <li>这些注解的默认错误消息都是预定义的, 也可以通过 message 属性自定义错误消息.</li>
 *     <li>这些注解支持校验的类型见各自的类注释.</li>
 *     <li>为了体现每个注解的单一职责, 除了 {@code @NotNull}, {@code @NotEmpty}, {@code @NotBlank} 之外, 其他注解都默认
 *     {@code null} 值合法, 需要判空的话, 显式地添加 {@code @NotNull} .</li>
 * </ul>
 */
@Data
public class BasicAnnotatedDTO {

    /**
     * {@code @Null}: 该字段必须为 {@code null}, 适用于任意对象
     */
    @Null(message = "fieldNull 字段必须为 null")
    private String fieldNull;

    /**
     * {@code @NotNull}: 该字段不能为 {@code null}, 适用于任意对象, 包括集合、数组、Map 等
     */
    @NotNull(message = "fieldNotNull 字段不能为 null")
    private String fieldNotNull;

    /**
     * {@code @NotEmpty}: 该字段不能为 {@code null}, 且不能为空.
     */
    @NotEmpty(message = "fieldNotEmpty 字符串不能为 null / 空字符串")
    private String fieldNotEmpty;

    @NotEmpty(message = "fieldNotEmptyList 列表不能为空")
    private List<String> fieldNotEmptyList;

    @NotEmpty(message = "fieldNotEmptyMap 映射不能为空")
    private Map<String, Object> fieldNotEmptyMap;

    /**
     * {@code @NotBlank}: 该字段不能为 {@code null}, 且不能为空, 且不能只包含空格.
     */
    @NotBlank(message = "fieldNotBlank 字符串不能为 null / 空字符串 / 空格")
    private String fieldNotBlank;

    /**
     * {@code @Size}: 该字段如果不为 {@code null}, 长度/大小不能小于指定值, 且不能大于指定值.
     */
    @Size(min = 1, max = 10, message = "fieldSize 字符串长度必须在 1 到 10 之间")
    private String fieldSize;

    @Size(min = 1, max = 100, message = "fieldSizeList 列表长度必须在 1 到 100 之间")
    private List<String> fieldSizeList;

    @Size(min = 1, max = 100, message = "fieldSizeMap 映射的大小必须在 1 到 100 之间")
    private Map<String, Object> fieldSizeMap;

    /**
     * 以下两个可以组合使用也可以单独使用.
     * <ul>
     *     <li>{@code @Min}: 该字段如果不为 {@code null}, 其值不能小于指定值</li>
     *     <li>{@code @Max}: 该字段如果不为 {@code null}, 其值不能大于指定值</li>
     * </ul>
     */
    @Min(value = 1, message = "fieldRange 字段值不能小于 0")
    @Max(value = 10, message = "fieldRange 字段值不能大于 150")
    private Integer fieldRange;

    /**
     * 以下两个可以组合使用也可以单独使用.
     * <ul>
     *     <li>{@code @DecimalMin}: 该字段如果不为 {@code null}, 其值不能小于指定值.</li>
     *     <li>{@code @DecimalMax}: 该字段如果不为 {@code null}, 其值不能大于指定值.</li>
     * </ul>
     */
    @DecimalMin(value = "0.01", message = "fieldDecimalRange 字段值不能小于 0.01")
    @DecimalMax(value = "1.00", message = "fieldDecimalRange 字段值不能大于 1.00")
    private BigDecimal fieldDecimalRange;

    /**
     * {@code @Positive}: 该字段如果不为 {@code null}, 其值不能小于 0.
     */
    @Positive(message = "fieldPositive 字段值必须为正数")
    private Integer fieldPositive;

    /**
     * {@code @PositiveOrZero}: 该字段如果不为 {@code null}, 其值不能为负数.
     */
    @PositiveOrZero(message = "fieldPositiveOrZero 字段值必须为非负数")
    private Integer fieldPositiveOrZero;

    /**
     * {@code @Negative}: 该字段值不能大于 0.
     */
    @Negative(message = "fieldNegative 字段值必须为负数")
    private Integer fieldNegative;

    /**
     * {@code @NegativeOrZero}: 该字段如果不为 {@code null}, 其值不能为正数
     */
    @NegativeOrZero(message = "fieldNegativeOrZero 字段值必须为非正数")
    private Integer fieldNegativeOrZero;

    /**
     * {@code @Digits}: 该字段如果不为 {@code null}, 其值必须符合指定的数字格式
     */
    @Digits(integer = 5, fraction = 2, message = "fieldDigits 字段值必须是 5 位整数和 2 位小数")
    private BigDecimal fieldDigits;

    /**
     * {@code @Email}: 该字段如果不为 {@code null}, 其值必须符合邮箱格式
     */
    @Email(message = "fieldEmail 邮箱格式不合法")
    private String fieldEmail;

    /**
     * {@code @Pattern}: 该字段如果不为 {@code null}, 其值必须符合指定的正则表达式
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "fieldPattern 手机号格式不合法")
    private String fieldPattern;

    /**
     * {@code @Past}: 该字段如果不为 {@code null}, 其值必须是过去的日期
     */
    @Past(message = "fieldPast 字段值必须是过去的日期")
    private LocalDate fieldPast;

    /**
     * {@code @PastOrPresent}: 该字段如果不为 {@code null}, 其值必须是过去的日期或今天
     */
    @PastOrPresent(message = "fieldPastOrPresent 字段值必须是过去的日期或今天")
    private LocalDate fieldPastOrPresent;

    /**
     * {@code @Future}: 该字段如果不为 {@code null}, 其值必须是将来的日期
     */
    @Future(message = "fieldFuture 字段值必须是将来的日期")
    private LocalDate fieldFuture;

    /**
     * {@code @FutureOrPresent}: 该字段如果不为 {@code null}, 其值必须是将来的日期或今天
     */
    @FutureOrPresent(message = "fieldFutureOrPresent 字段值必须是将来的日期或今天")
    private LocalDate fieldFutureOrPresent;

    /**
     * {@code @AssertTrue}: 该字段值必须为 {@code true}
     */
    @AssertTrue(message = "fieldBoolean 字段值必须为 true")
    private boolean fieldBoolean;

    /**
     * {@code @AssertFalse}: 该字段值必须为 {@code false}
     */
    @AssertFalse(message = "fieldAssertFalse 字段值必须为 false")
    private boolean fieldAssertFalse;
}
