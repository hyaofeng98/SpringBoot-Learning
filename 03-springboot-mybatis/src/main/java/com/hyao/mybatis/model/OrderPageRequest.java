package com.hyao.mybatis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class OrderPageRequest {
    private String userId;

    @Min(value = 0, message = "状态值不能小于 0")
    @Max(value = 4, message = "状态值不能大于 4")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdTo;

    private Integer minAmount;

    private Integer maxAmount;

    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不能小于 1")
    @Max(value = 100, message = "分页大小不能超过 100")
    private Integer pageSize;

    private String lastId;
}
