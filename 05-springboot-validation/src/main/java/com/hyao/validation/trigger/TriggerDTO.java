package com.hyao.validation.trigger;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@Data
public class TriggerDTO {

    @NotNull
    @Max(value = 100, message = "不能超过 100")
    private Integer id;
}
