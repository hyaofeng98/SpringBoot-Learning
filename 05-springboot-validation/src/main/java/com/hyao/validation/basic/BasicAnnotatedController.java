package com.hyao.validation.basic;

import com.hyao.common.model.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 基本注解测试.
 */
@RestController
@RequestMapping("/valid")
public class BasicAnnotatedController {

    /**
     * <pre>
     *     请求示例:
     *     POST /basic/valid
     *
     *     Content-Type: application/json
     *     { "fieldNotNull": "hyao", "fieldRange": 18 }
     * </pre>
     */
    @PostMapping("/basic")
    public Result<Void> validate(@Valid @RequestBody BasicAnnotatedDTO dto) {
        return Result.ok();
    }

}
