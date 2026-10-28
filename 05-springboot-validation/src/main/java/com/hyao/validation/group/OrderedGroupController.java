package com.hyao.validation.group;

import com.hyao.common.model.Result;
import com.hyao.validation.group.dto.OrderedGroupDTO;
import com.hyao.validation.group.groups.OrderedGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 分组顺序校验示例.
 * <p>让校验规则按预定顺序执行.
 * <ul>
 *     <li>{@code @Validated}: 在 {@code Controller} 类上加上该注释, 并指定一个定义校验分组的顺序的分组(例如本示例中指定了:
 *     {@code OrderedGroup.class} ), 即可使顺序校验生效.</li>
 * </ul>
 *
 * @see OrderedGroup
 * @see OrderedGroupDTO
 */
@Validated(OrderedGroup.class)
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
    public Result<Void> validation(@Valid @RequestBody OrderedGroupDTO orderedGroupDTO) {
        return Result.ok();
    }

}
