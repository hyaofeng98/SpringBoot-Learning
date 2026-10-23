package com.hyao.validation.custom;

import com.hyao.common.model.Result;
import com.hyao.validation.custom.dto.CustomDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
     *     { "phone": "1234567890", "password": "123456", "confirmPassword": "12345611"}
     * </pre>
     */
    @PostMapping("/custom")
    public Result<Void> validation(@Valid @RequestBody CustomDTO customDTO) {
        return Result.ok();
    }

}
