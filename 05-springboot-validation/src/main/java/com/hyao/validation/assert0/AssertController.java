package com.hyao.validation.assert0;

import com.hyao.common.model.Result;
import com.hyao.validation.assert0.dto.AssertDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 演示 {@code @AssertTrue} 和 {@code @AssertFalse} 注解的使用.
 *
 * @see AssertDTO
 */
@RestController
@RequestMapping("/valid")
public class AssertController {

    /**
     * <pre>
     *     请求示例:
     *     POST /valid/assert0
     *
     *     Content-Type: application/json
     *     { "begin": "2022-01-01", "end": "2021-01-03", "password": "123456", "confirmPassword": "1234567" }
     * </pre>
     */
    @PostMapping("/assert0")
    public Result<Void> validation(@Valid @RequestBody AssertDTO assertDTO) {
        return Result.ok();
    }
}
