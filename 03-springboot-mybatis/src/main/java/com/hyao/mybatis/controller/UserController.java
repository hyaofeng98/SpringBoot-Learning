package com.hyao.mybatis.controller;

import com.hyao.mybatis.model.UserPageQuery;
import com.hyao.mybatis.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息相关控制器.
 *
 * <p>参数校验交给 00 模块的 Bean Validation：需要校验的接口在参数上标 {@code @Valid}（见 {@code /pageQueryConditional}），
 * 失败时统一返回 {@code 400 + SYS0004}。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 插入用户信息.
     * <pre>
     *     请求示例:
     *     POST /user/insert
     *
     *     Content-Type: application/json
     *     { "nickname": "hyao", "avatar": "", "birthday": "2000-01-01" }
     * </pre>
     *
     * @param params 待插入的用户信息
     * @return 插入结果
     */
    @PostMapping("/insert")
    public Map<String, Object> insertUser(@RequestBody Map<String, Object> params) {
        // 参数校验...
        userService.insertUser(params);
        Map<String, Object> map = new HashMap<>();
        map.put("result", true);
        return map;
    }

    /**
     * 根据 id 查询用户信息.
     *
     * <pre>
     *     请求示例:
     *     POST /user/queryById
     *
     *     Content-Type: application/json
     *     { "id": "u0000001" }
     * </pre>
     *
     * @param params 查询参数
     * @return 查询结果
     */
    @PostMapping("/queryById")
    public Map<String, Object> queryById(@RequestBody Map<String, Object> params) {
        // 参数校验...
        String id = (String) params.get("id");
        return userService.queryUserById(id);
    }

    /**
     * 根据条件查询用户信息.
     *
     * <p>{@code @Valid} 让 00 模块的 Bean Validation 生效：{@code pageSize} 必填且 1~100，
     * {@code beginDate}/{@code endDate} 要求 {@code yyyy-MM-dd}；校验失败返回 {@code 400 + SYS0004}。
     * <pre>
     *     请求示例:
     *     POST /user/pageQueryConditional
     *
     *     Content-Type: application/json
     *     { "status": 1, "gender": 1, "beginDate": "1980-01-01", "endDate": "2000-01-01", "pageSize": 10 }
     * </pre>
     * @param query 查询参数
     * @return 查询结果
     */
    @PostMapping("/pageQueryConditional")
    public Map<String, Object> pageQueryConditional(@Valid @RequestBody UserPageQuery query) {
        return userService.pageQueryUserConditional(query);
    }

    /**
     * 更新用户昵称.
     *
     * <pre>
     *     请求示例:
     *     POST /user/update
     *
     *     Content-Type: application/json
     *     { "nickname": "hyao feng", "avatar": "123", "id": "u0000001" }
     * </pre>
     *
     * @param params 待更新的用户信息
     * @return 更新结果
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> params) {
        boolean result = userService.updateById(params) == 1;
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        return map;
    }

    /**
     * 删除用户信息.
     * <pre>
     *     请求示例:
     *     POST /user/delete
     *
     *     Content-Type: application/json
     *     { "id": "u0000001" }
     * </pre>
     *
     * @param params 待删除的用户信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteUser(@RequestBody Map<String, Object> params) {
        // 参数校验...
        boolean result = userService.deleteUser((String) params.get("id")) == 1;
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        return map;
    }
}
