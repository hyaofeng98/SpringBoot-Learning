package com.hyao.validation.group;

import com.hyao.common.model.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分组校验示例.
 */
@RestController
@RequestMapping("/valid/group")
public class GroupController {

    /**
     * {@code CreateGroup} 分组校验示例
     * <pre>
     *     请求示例:
     *     POST /group/create
     *
     *     Content-Type: application/json
     *     { id: 123, "name": "hyao", "remark": "" }
     * </pre>
     */
    @PostMapping("/create")
    public Result<Void> create(@Validated(CreateGroup.class) @RequestBody GroupDTO dto) {
        return Result.ok();
    }

    /**
     * {@code UpdateGroup} 分组校验示例
     * <pre>
     *     请求示例:
     *     POST /group/update
     *
     *     Content-Type: application/json
     *     { "name": "hyao", "remark": "" }
     * </pre>
     */
    @PostMapping("/update")
    public Result<Void> update(@Validated(UpdateGroup.class) @RequestBody GroupDTO dto) {
        return Result.ok();
    }

    /**
     * 默认分组校验示例
     * <pre>
     *     请求示例:
     *     POST /group/default
     *
     *     Content-Type: application/json
     *     { id: 123, "name": "hyao", "remark": "" }
     * </pre>
     */
    @PostMapping("/default")
    public Result<Void> defaultGroup(@Validated @RequestBody GroupDTO dto) {
        return Result.ok();
    }
}
