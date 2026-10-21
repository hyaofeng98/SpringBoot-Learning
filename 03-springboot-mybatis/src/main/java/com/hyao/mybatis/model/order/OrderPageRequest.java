package com.hyao.mybatis.model.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 订单分页查询请求.
 */
@Data
public class OrderPageRequest {

    /** 用户id */
    private String userId;

    /** 订单状态 */
    @Min(value = 0, message = "状态值不能小于 0")
    @Max(value = 4, message = "状态值不能大于 4")
    private Integer status;

    /** 起始创建日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdFrom;

    /** 结束创建日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdTo;

    /** 最小订单金额 */
    private Integer minAmount;

    /** 最大订单金额 */
    private Integer maxAmount;

    /** 分页大小 */
    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不能小于 1")
    @Max(value = 100, message = "分页大小不能超过 100")
    private Integer pageSize;

    /** 当前页起始游标 */
    private String cursor;
}
