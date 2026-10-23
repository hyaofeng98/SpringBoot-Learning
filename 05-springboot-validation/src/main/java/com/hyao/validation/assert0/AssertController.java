package com.hyao.validation.assert0;

import com.hyao.common.model.Result;
import com.hyao.validation.assert0.dto.AssertDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/valid")
public class AssertController {

    /**
     * 演示 {@code @AssertTrue} 和 {@code @AssertFalse} 注解的使用.
     * @param assertDTO
     * @return
     */
    @PostMapping("/assert0")
    public Result<Void> validation(@Valid @RequestBody AssertDTO assertDTO) {
        return Result.ok();
    }
}
