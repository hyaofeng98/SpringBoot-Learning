package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.trigger.TriggerDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;

/**
 * 2.请求参数校验的四种方式.
 * <p>第 3、4 种单参数校验在 Spring 6.1+ 由 Spring MVC 内置的方法校验负责（{@code HandlerMethodValidationException}），
 * <b>不再需要</b>类级 {@code @Validated}；只有类上显式标了 {@code @Validated} 时才改走 AOP 代理
 * （{@code ConstraintViolationException}）。</p>
 */
@RestController
@RequestMapping("/valid/trigger")
public class TriggerController {

    /**
     * 方式一: 通过方法参数 {@code @RequestBody} + {@code @Valid} 进行验证.
     * <pre>
     *     请求示例:
     *     POST /valid/trigger/body1
     *
     *     Content-Type: application/json
     *     { "id": 123 }
     * </pre>
     */
    @PostMapping("/body1")
    public Result<Void> byBody1(@Valid @RequestBody TriggerDTO triggerDTO) {
        return Result.ok();
    }

    /**
     * 方式二: 通过方法参数 {@code @RequestBody} + {@code @Validated} 进行验证.
     * <pre>
     *     请求示例:
     *     POST /valid/trigger/body2
     *
     *     Content-Type: application/json
     *     { "id": 123 }
     * </pre>
     */
    @PostMapping("/body2")
    public Result<Void> byBody2(@Validated @RequestBody TriggerDTO triggerDTO) {
        return Result.ok();
    }

    /**
     * 方式三: 通过 {@code @RequestParam} 单参数验证.
     * <p>本类没有类级 {@code @Validated}, 校验由 Spring MVC 6.1+ 内置方法校验执行,
     * 失败抛 {@code HandlerMethodValidationException}（HTTP 400 + SYS0004）。</p>
     * <pre>
     *     请求示例:
     *     GET /valid/trigger/param?id=111
     * </pre>
     */
    @GetMapping("/param")
    public Result<Void> byParam(@RequestParam("id") @Max(value = 100, message = "id 不能超过 100") Integer id) {
        return Result.ok();
    }

    /**
     * 方式四: 通过 {@code @PathVariable} 单参数验证.
     * <p>触发方式同方式三（无需类级 {@code @Validated}）。</p>
     * <pre>
     *     请求示例:
     *     GET /valid/trigger/path/111
     * </pre>
     */
    @GetMapping("/path/{id}")
    public Result<Void> byPath(@PathVariable("id") @Max(value = 100, message = "id 不能超过 100") Integer id) {
        return Result.ok();
    }
}
