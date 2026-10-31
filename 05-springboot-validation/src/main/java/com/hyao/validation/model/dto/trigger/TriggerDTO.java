package com.hyao.validation.model.dto.trigger;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

@Data
public class TriggerDTO {

    @NotNull(message = "id 不能为空")
    @Max(value = 100, message = "id 长度不能超过 100")
    private Integer id;
}
