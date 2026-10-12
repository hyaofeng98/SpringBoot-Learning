package com.hyao.advice.controller;

import com.hyao.advice.advice.IgnoreWrapper;
import com.hyao.advice.common.Result;
import com.hyao.advice.model.User;
import org.springframework.web.bind.annotation.*;

/**
 * 正常返回示例。
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    /**
     * 查询用户。
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
     * 查询用户。
     *
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
     * 查询用户。
     *
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
