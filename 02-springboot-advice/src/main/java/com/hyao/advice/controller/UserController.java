package com.hyao.advice.controller;

import com.hyao.advice.model.User;
import com.hyao.common.advice.IgnoreWrapper;
import com.hyao.common.model.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 正常返回示例。
 */
@RestController
@RequestMapping("/api")
public class UserController {

    /**
     * 演示返回字符串响应的数据包装效果。
     * <pre>
     *     请求示例：
     *     GET http://localhost:8081/api/hello
     * </pre>
     * @return 字符串响应
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    /**
     * 演示返回POJO对象响应的数据包装效果。
     * <pre>
     *     请求示例：
     *     POST http://localhost:8081/api/user1
     *
     *     Content-Type: application/json
     *     { "id": 1, "name": "hyao", "age": 18, "birthday": "2000-01-01" }
     * </pre>
     *
     * @param user 用户信息
     * @return 未包装的用户信息
     */
    @PostMapping("/user1")
    public User user1(@RequestBody User user) {
        // 查询用户的逻辑 ...
        return User.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .birthday(user.getBirthday())
                .build();
    }

    /**
     * 演示 {@code @IgnoreWrapper} 跳过包装的效果。
     * <pre>
     *     请求示例：
     *     POST http://localhost:8081/api/user2
     *
     *     Content-Type: application/json
     *     { "id": 1, "name": "hyao", "age": 18, "birthday": "2000-01-01" }
     * </pre>
     * @param user 用户信息
     * @return 未包装的用户信息，跳过包装
     */
    @PostMapping("/user2")
    @IgnoreWrapper
    public User user2(@RequestBody User user) {
        // 查询用户的逻辑 ...
        return User.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .birthday(user.getBirthday())
                .build();
    }

    /**
     * 演示返回结果已包装响应的包装效果。
     * <pre>
     *     请求示例：
     *     POST http://localhost:8081/api/user3
     *
     *     Content-Type: application/json
     *     { "id": 1, "name": "hyao", "age": 18, "birthday": "2000-01-01" }
     * </pre>
     * @param user 用户信息
     * @return 已包装的用户信息
     */
    @PostMapping("/user3")
    public Result<User> user3(@RequestBody User user) {
        // 查询用户的逻辑 ...
        User queryUser = User.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .birthday(user.getBirthday())
                .build();
        return Result.ok(queryUser);
    }
}
