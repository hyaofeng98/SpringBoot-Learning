package com.hyao.base.controller;

import com.hyao.base.model.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SpringBoot REST风格控制器示例。主要用于返回JSON/文本的API接口。
 * <li>
 *     {@code @RestController}是组合注解，等价于{@code @Controller} + {@code @ResponseBody}。
 *     加了{@code @ResponseBody}注解后类中每个方法的返回值都会被直接序列化后写入 HTTP 响应体，而不再当
 *     作视图名交给视图解析器解析。
 * </li>
 * <li>
 *     {@code @RequestMapping} 注解和{@code MyController}中一样，注解在类上，表示该Controller中
 *     所有方法的公共路径前缀。
 * </li>
 */
@RestController
@RequestMapping("/api")
public class MyRestController {

    /**
     * 直接返回字符串。
     *
     * <p>{@code @GetMapping("/hello")} 是 @RequestMapping(value="/hello",
     * method=RequestMethod.GET) 的简写，
     * 只处理 GET 请求。其他对应的请求还有：
     * <li>{@code @PostMapping}</li>
     * <li>{@code @PutMapping}</li>
     * <li>{@code @DeleteMapping}</li>
     * <li>{@code @PatchMapping}</li>
     *
     * <pre>
     *     调用示例：
     *     POST /api/hello
     * </pre>
     *
     * @return 返回字符串，返回String时由 {@code StringHttpMessageConverter} 写回， Content-Type为 text/plain。
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    /**
     * 直接返回 JSON 对象。
     * <pre>
     *      调用示例：
     *      POST /api/user
     * </pre>
     *
     * @return 用户信息 Map，由 {@code MappingJackson2HttpMessageConverter} 写回，Content-Type为 application/json.
     */
    @PostMapping("/user")
    public Map<String, Object> user() {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", "0001");
        userMap.put("name", "hyao");
        userMap.put("age", 18);
        // 关于 LocalDate 日期类的序列化格式，需要另外配置jackson
        userMap.put("birthday", LocalDate.now().plusYears(-18));
        return userMap;
    }

    /**
     * 路径变量 + {@code @PathVariable} 绑定示例。
     * <li>
     *     {@code @PostMapping("/user/{id}")} 中的 {@code {id}} 是一个路径占位符，表示这一段路径的值是动态的。
     * </li>
     * <pre>
     *      调用示例：
     *      POST /api/user/0001
     * </pre>
     *
     * @param id 用户id，{@code @PathVariable("id") String id} — 把路径变量绑定到方法参数。比如访问 {@code /user/0001}
     *           时，{@code id} 参数的值就是 {@code 0001}。路径变量默认必填，如果需要可选，可以使用
     *           {@code @PathVariable(value="id", required=false) String id}。
     * @return 对应用户信息
     */
    @PostMapping("/user/{id}")
    public Map<String, Object> userById(@PathVariable("id") String id) {
        // 查询用户的逻辑 ...
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", id);
        userMap.put("name", "hyao");
        return userMap;
    }

    /**
     * 请求参数 + {@code @RequestParam} 绑定示例。
     * <pre>
     *     调用示例：
     *     POST /api/user/param?id=0001&name=hyao
     * </pre>
     *
     * @param id   用户id，{@code @RequestParam String id} — 把请求参数绑定到方法参数。比如访问 {@code /user/param?id=0001}
     *             时，{@code id} 参数的值就是 {@code 0001}。
     * @param name 用户姓名
     * @return 用户信息
     */
    @PostMapping("/user/param")
    public Map<String, Object> userByParam(@RequestParam String id, @RequestParam(required = false) String name) {
        // 查询用户的逻辑 ...
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", id);
        userMap.put("name", name);
        return userMap;
    }

    /**
     * 请求体 + {@code @RequestBody} 绑定示例1。
     * <pre>
     *     调用示例：
     *     POST /api/user/body/map
     *     Content-Type: application/json
     *
     *     { "id": "0001", "name": "hyao", "age": 18, "birthday": "2008-10-11" }
     * </pre>
     *
     * @param params 请求参数，{@code @RequestBody Map<String, Object> params} — 把请求体绑定到方法参数。请求体中的 JSON
     *               数据会反序列化为 Map。
     * @return 用户信息
     */
    @PostMapping("/user/body/map")
    public Map<String, Object> userByRequestBody(@RequestBody Map<String, Object> params) {
        // 查询用户的逻辑 ...
        return new LinkedHashMap<>(params);
    }

    /**
     * 请求体 + {@code @RequestBody} 绑定示例2。
     * <pre>
     *     调用示例：
     *     POST /api/user/body/object
     *     Content-Type: application/json
     *
     *     { "id": "0001", "name": "hyao", "age": 18, "birthday": "2008-10-11" }
     * </pre>
     *
     * @param user 请求参数{@code @RequestBody User user} — 把请求体绑定到方法参数。请求体中的 JSON既然能反序列化为Map，那么也
     *             能反序列化为 POJO对象。
     *             <p><b>注意：如果POJO对象只写 @Builder 而不加 @NoArgsConstructor，Lombok 会导致没有无参构造，而Jackson 反
     *             序列化必须先 new 一个空对象，从而导致反序列化报错。详见{@link User}。</b>
     * @return 用户信息，返回类型也可以是 POJO对象，框架会自动序列化为 JSON。
     */
    @PostMapping("/user/body/object")
    public User userByRequestBody(@RequestBody User user) {
        // 查询用户的逻辑 ...
        return User.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .birthday(user.getBirthday())
                .build();
    }
}
