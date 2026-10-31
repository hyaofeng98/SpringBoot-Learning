package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.custom.CustomDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 4.自定义校验示例. 已有的校验注解无法满足需求时, 可以自定义校验注解和校验器.
 *
 * @see CustomDTO
 */
@RestController
@RequestMapping("/valid")
public class CustomController {

    /**
     * 自定义校验演示.
     * <pre>
     *     请求示例:
     *     POST /valid/custom
     *
     *     Content-Type: application/json
     *     { "username": "hyao", "phone": "1341123", "password": "123456", "confirmPassword": "12345116" }
     * </pre>
     */
    @PostMapping("/custom")
    public Result<Void> validation(@Valid @RequestBody CustomDTO customDTO) {
        return Result.ok();
    }

}
