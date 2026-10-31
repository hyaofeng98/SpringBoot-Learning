package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.group.OrderedGroupDTO;
import com.hyao.validation.validation.group.OrderedGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 8.分组顺序校验示例.
 * <p>让校验规则按预定顺序执行.
 * <ul>
 *     <li>{@code @Validated}: 在 {@code Controller} 类上加上该注释, 并指定一个定义校验分组的顺序的分组(例如本示例中指定了:
 *     {@code OrderedGroup.class} ), 即可使顺序校验生效.</li>
 * </ul>
 *
 * @see OrderedGroup
 * @see OrderedGroupDTO
 */
@RestController
@RequestMapping("/valid")
public class OrderedGroupController {

    /**
     * 分组顺序校验演示.
     * <pre>
     *     请求示例:
     *     POST /valid/orderedGroup
     *
     *     Content-Type: application/json
     *     { "id": "123", "name": "aaacccs", "address": "345", "phone": "111222333444" }
     * </pre>
     */
    @PostMapping("/orderedGroup")
    public Result<Void> validation(@Validated(OrderedGroup.class) @RequestBody OrderedGroupDTO orderedGroupDTO) {
        return Result.ok();
    }

}
