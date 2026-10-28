package com.hyao.mybatis.model.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 订单统计请求参数。
 */
@Data
public class OrderStatisticsRequest {

    /** 统计开始日期 */
    @NotNull(message = "统计开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;

    /** 统计结束日期 */
    @NotNull(message = "统计结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 统计维度 */
    @NotBlank(message = "统计维度不能为空")
    @Pattern(regexp = "day|status", message = "统计维度只能是 day 或 status")
    private String dimension;

    /** 订单状态 */
    @Min(value = 0, message = "订单状态最小值为 0")
    @Max(value = 4, message = "订单状态最大值为 4")
    private Integer status;

    /**
     * 验证开始日期是否晚于结束日期。
     *
     * @return 是否验证通过
     */
    @AssertTrue(message = "开始日期不能晚于结束日期")
    public boolean isDateRangeValid() {
        return beginDate == null || endDate == null || !beginDate.isAfter(endDate);
    }

    /**
     * 验证查询跨度是否超过 90 天。
     *
     * @return 是否验证通过
     */
    @AssertTrue(message = "查询跨度不能超过 90 天")
    public boolean isRangeWithinLimit() {
        return beginDate == null || endDate == null || ChronoUnit.DAYS.between(beginDate, endDate) <= 90;
    }
}
