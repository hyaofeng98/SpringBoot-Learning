package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.custom.CustomDTO;
import com.hyao.validation.service.ProgrammaticService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 11.编程式校验演示.
 *
 * @see ProgrammaticService
 */
@RestController
@RequestMapping("/valid")
public class ProgrammaticController {

    private final ProgrammaticService programmaticService;

    public ProgrammaticController(ProgrammaticService programmaticService) {
        this.programmaticService = programmaticService;
    }

    /**
     * 编程式校验演示.
     * <pre>
     *     请求示例:
     *     POST /valid/programmatic
     *
     *     Content-Type: application/json
     *     { "phone": "1234567890", "password": "123456", "confirmPassword": "12345611"}
     * </pre>
     */
    @PostMapping("/programmatic")
    public Result<Void> validation(@RequestBody CustomDTO customDTO) {
        programmaticService.validate(customDTO);
        return Result.ok();
    }

}
