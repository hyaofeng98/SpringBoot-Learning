package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.group.DynamicDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 9.动态分组校验示例.
 * <p>根据对象自身的字段值，在运行时动态决定要执行哪些校验规则, 用于解决条件必填的场景. 动态分组是 Hibernate Validator 的实现扩展.
 *
 * @see DynamicDTO
 */
@RestController
@RequestMapping("/valid")
public class DynamicGroupController {

    /**
     * 动态分组校验演示.
     * <pre>
     *     请求示例:
     *     POST /valid/dynamicGroup
     *
     *     Content-Type: application/json
     *     { "type": "1", "description": "" }
     * </pre>
     */
    @PostMapping("/dynamicGroup")
    public Result<Void> validation(@Valid @RequestBody DynamicDTO dynamicDTO) {
        return Result.ok();
    }
}
