package com.hyao.validation.group;

import com.hyao.common.model.Result;
import com.hyao.validation.group.dto.ConvertGroupDTO;
import com.hyao.validation.group.groups.OuterCreateGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 级联分组转换示例.
 *
 * @see ConvertGroupDTO
 */
@RestController
@RequestMapping("/valid")
public class ConvertGroupController {

    /**
     * 外部创建分组.
     * <pre>
     *     请求示例:
     *     POST /valid/convertGroup
     *
     *     Content-Type: application/json
     *     { "dto": { "id": 123, "name": "hyao", "remark": "" } }
     * </pre>
     */
    @PostMapping("/convertGroup")
    public Result<Void> validation(@Validated(OuterCreateGroup.class) @RequestBody ConvertGroupDTO convertGroupDTO) {
        return Result.ok();
    }
}
