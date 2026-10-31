package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.nested.BaseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 6.嵌套校验示例.
 *
 * @see BaseDTO
 */
@RestController
@RequestMapping("/valid")
public class NestedController {

    /**
     * 演示嵌套校验的效果.
     * <pre>
     *     请求示例:
     *     POST /valid/nested
     *
     *     Content-Type: application/json
     *     { "nested": { "id": "12345" }, "list1": [{ "id": "123456" }], "list2": [{ "id": "123456" }], "list3": [ "111", "" ], "map": { "id": "" } }
     * </pre>
     */
    @PostMapping("/nested")
    public Result<Void> nested(@Valid @RequestBody BaseDTO baseDTO) {
        return Result.ok();
    }

}
