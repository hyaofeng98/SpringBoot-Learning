package com.hyao.validation.trigger;

import com.hyao.common.model.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;

/**
 * 请求参数校验的三种方式.
 * 其中第 3,4 种单参数校验方式依赖类级的 {@code @Validated}, 否则不会生效.
 */
@Validated
@RestController
@RequestMapping("/valid/trigger")
public class TriggerController {

    /**
     * 方式一: 通过方法参数 {@code @RequestBody} + {@code @Valid} 进行验证.
     * <pre>
     *     请求示例:
     *     POST /trigger/body1
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
     *     POST /trigger/body
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
     * 方式三: 通过 {@code @RequestParam} 单参数验证 (依赖类级 {@code @Validated} 注解).
     * <pre>
     *     请求示例:
     *     GET /trigger/param?id=111
     * </pre>
     */
    @GetMapping("/param")
    public Result<Void> byParam(@RequestParam("id") @Max(value = 100, message = "不能超过 100") Integer id) {
        return Result.ok();
    }

    /**
     * 方式四: 通过 {@code @PathVariable} 单参数验证 (依赖类级 {@code @Validated} 注解).
     * <pre>
     *     请求示例:
     *     GET /trigger/path/111
     * </pre>
     */
    @GetMapping("/path/{id}")
    public Result<Void> byPath(@PathVariable("id") @Max(value = 100, message = "不能超过 100") Integer id) {
        return Result.ok();
    }
}
