package com.hyao.validation.controller;

import com.hyao.common.model.Result;
import com.hyao.validation.model.dto.group.GroupDTO;
import com.hyao.validation.validation.group.CreateGroup;
import com.hyao.validation.validation.group.UpdateGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 7.分组校验示例.
 * <p>用于解决同一个对象在不同场景下校验规则不同的问题, JSR-303（jakarta.validation）通过 groups 机制解决这个问题.
 *
 * @see GroupDTO
 */
@RestController
@RequestMapping("/valid/group")
public class GroupController {

    /**
     * {@code CreateGroup} 分组校验示例
     * <pre>
     *     请求示例:
     *     POST /valid/group/create
     *
     *     Content-Type: application/json
     *     { "id": 123, "name": "hyao", "remark": "" }
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
     *     POST /valid/group/update
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
     *     POST /valid/group/default
     *
     *     Content-Type: application/json
     *     { "id": 123, "name": "hyao", "remark": "" }
     * </pre>
     */
    @PostMapping("/default")
    public Result<Void> defaultGroup(@Validated @RequestBody GroupDTO dto) {
        return Result.ok();
    }
}
