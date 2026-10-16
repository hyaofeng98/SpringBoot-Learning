package com.hyao.mybatis.controller;

import com.hyao.mybatis.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息相关控制器.
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
     *     { "nickname": "hyao", "avatar": "" }
     * </pre>
     *
     * @param params 待插入的用户信息
     * @return 插入结果
     */
    @PostMapping("/insert")
    public Map<String, Object> insertUser(@RequestBody Map<String, Object> params) {
        // 参数校验...
        boolean result = userService.insertUser(params) == 1;
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        return map;
    }

    /**
     * 根据 id 查询用户信息.
     *
     * <pre>
     *     请求示例:
     *     POST /user/queryByNickname
     *
     *     Content-Type: application/json
     *     { "nickname": "hyao" }
     * </pre>
     *
     * @param params 查询参数
     * @return 查询结果
     */
    @PostMapping("/queryByNickname")
    public Map<String, Object> queryByNickname(@RequestBody Map<String, Object> params) {
        // 参数校验...
        String nickname = (String) params.get("nickname");
        return userService.queryByNickname(nickname);
    }

    /**
     * 更新用户昵称.
     *
     * <pre>
     *     请求示例:
     *     POST /user/updateNickname
     *
     *     Content-Type: application/json
     *     { "nickname": "hyao feng", "id": "u0000001" }
     * </pre>
     *
     * @param params 待更新的用户信息
     * @return 更新结果
     */
    @PostMapping("/updateNickname")
    public Map<String, Object> updateNickname(@RequestBody Map<String, Object> params) {
        // 参数校验...
        boolean result = userService.updateNickname((String) params.get("id"), (String) params.get("nickname")) == 1;
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
