# 01-springboot-base

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 1. 学习目标

| 目标 | 落点 |
| --- | --- |
| 启动类、自动配置与包扫描范围 | `BaseApplication` |
| 配置文件、静态资源规则 | `application.yaml`、`resources/static/index.html` |
| 视图控制器：视图名、重定向与转发 | `MyController` |
| REST 控制器：参数绑定、JSON 序列化 | `MyRestController`、`UserEntity` |

## 2. 学习清单

| 状态 | 优先级 | 知识点 | 说明 |
| --- | --- | --- | --- |
| ✅ | — | **启动类与包扫描** | `@SpringBootApplication` 三合一（`@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`）；默认只扫启动类所在包及子包，Controller 写到 `com.hyao.base` 之外且不配 `scanBasePackages` 就是 404 |
| ✅ | — | **配置与静态资源** | `server.port`（不配默认 8080，本模块当前 8081）；`spring.mvc.view.prefix/suffix` 决定 `prefix + 视图名 + suffix`；`static/` 四个默认位置有优先级，放进去就能访问、不经 Controller 也不走视图解析器 |
| ✅ | — | **视图控制器：视图名、转发与重定向** | `@Controller` 返回字符串 = 视图名，交给 `InternalResourceViewResolver`；类级 + 方法级 `@RequestMapping`（路径拼接、`path`/`value` 等价、`method` 限定）；`redirect:`（302、地址变）与 `forward:`（内部转发、地址不变） |
| ✅ | — | **REST 控制器与参数绑定** | `@RestController` = `@Controller` + `@ResponseBody`；`@GetMapping` 等是 `@RequestMapping(method = …)` 的简写（用错方法 405）；`@PathVariable` / `@RequestParam` / `@RequestBody`（`Map` 和 POJO 都能接，**一个方法只能有一个**） |
| ✅ | — | **消息转换与 JSON 序列化** | `HttpMessageConverter`：`String` → `StringHttpMessageConverter`（text/plain），POJO/`Map` → `MappingJackson2HttpMessageConverter`（application/json）；Lombok 四注解（缺无参构造会让 Jackson 反序列化失败）；`LocalDate` 默认 ISO-8601，`@JsonFormat` 改格式 |
| ✅ | — | **devtools 热部署** | 编译后自动重启上下文，改静态资源一般不用重启 |
| ⬜ | ★★★ | **参数校验与测试** | `@Valid` / `@Validated` 的声明式校验与错误信息处理（本模块没有校验示例）；`@SpringBootTest` / `@WebMvcTest` + `MockMvc` 做接口自动化断言（本模块没有测试代码） |
| ⬜ | ★★☆ | **`ResponseEntity` / `@ResponseStatus`、拦截器与过滤器** | 自定义 HTTP 状态码与响应头；`HandlerInterceptor`（Spring MVC 层）与 `Filter`（Servlet 层）的差异与执行顺序 |
| ⬜ | ★★☆ | **多环境配置 + Thymeleaf** | `application-dev/prod.yaml` + `spring.profiles.active`；视图解析器换成模板引擎的解析器 |
| ⬜ | ★☆☆ | **`@RequestHeader` / `@CookieValue`、文件上传下载** | 绑定请求头与 Cookie；`MultipartFile` 上传，下载可参考 02 模块的 `ResponseEntity<Resource>` |

## 3. 依赖与配置

### 3.1 依赖

本模块 `pom.xml` **只声明了 `parent`**，没有任何 `<dependencies>` 和 `<version>`：

| 依赖 | 来源 | 作用 |
| --- | --- | --- |
| `spring-boot-starter-web` | 父 POM 继承 | 内嵌 Tomcat + Spring MVC + Jackson（`@RestController`、消息转换器都靠它） |
| `spring-boot-devtools` | 父 POM 继承（runtime / optional） | 热部署：编译后自动重启上下文 |
| `lombok` | 父 POM 继承（provided / optional） | 编译期生成 getter/setter/构造器，`UserEntity` 用到 |

### 3.2 `application.yaml`

```yaml
server:
  port: 8081          # 不配置默认 8080
spring:
  mvc:
    view:
      prefix: /       # 视图解析器前缀
      suffix: .html   # 视图解析器后缀
```

YAML 用缩进表示层级（不能用 Tab）；`application.yaml` 与 `application.properties` 二选一，放 `resources/` 根目录或 `resources/config/` 下都能识别。`server.port` 是内嵌 Tomcat 端口，同一端口只能跑一个实例。

### 3.3 静态资源目录

`static/index.html` 不需要任何 Controller，直接访问 `http://localhost:<port>/index.html`。默认位置**按优先级**（前面的覆盖后面的）为 `classpath:/META-INF/resources/` → `classpath:/resources/` → `classpath:/static/`（本项目用的目录）→ `classpath:/public/`，由 `ResourceHttpRequestHandler` 处理、**不走视图解析器**。

## 4. 核心实现

### 4.1 `BaseApplication`：启动类与包扫描

```java
@SpringBootApplication
public class BaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaseApplication.class, args);
    }
}
```

- `@SpringBootApplication` = `@SpringBootConfiguration`（本质 `@Configuration`）+ `@EnableAutoConfiguration`（按 classpath 依赖自动装配 Bean：有 web 起步依赖就配好 Tomcat + Spring MVC + Jackson）+ `@ComponentScan`（**默认只扫启动类所在包及其子包**）；
- 启动类在 `com.hyao.base`，所以 `controller`、`model` 都会被扫到；`SpringApplication.run()` 创建容器、启动内嵌 Tomcat 并阻塞主线程。

### 4.2 `MyController`：视图控制器

```java
@Controller
@RequestMapping("/hello")
public class MyController {
    @RequestMapping(path = "/redirect", method = RequestMethod.GET)
    public String helloByRedirect() { return "redirect:/index.html"; }   // 302，地址栏变
    @RequestMapping("/view")
    public String helloByView() { return "index"; }                      // 视图名 → /index.html
}
```

- `@Controller` 方法返回**字符串表示视图名**，不直接写响应体；本项目没引入模板引擎，走默认的 `InternalResourceViewResolver`，把视图名拼成转发路径（`prefix + 视图名 + suffix`），返回 `"index"` 即转发到 `/index.html`；
- 类级 `@RequestMapping` 是公共前缀、方法级是具体路径，**两者拼接**；方法级常用 `path`/`value`（路径，等价）与 `method`（限定请求方法）。`/redirect` 显式写了 `RequestMethod.GET` 只能收 GET，`/forward`、`/view` 没写则**任意方法都能访问**；
- `redirect:` / `forward:` 前缀会让返回值**不再经过视图解析器**：前者 302 + `Location`、浏览器再发一次请求；后者服务器内部转发。三者最终渲染的都是 `static/index.html`。

### 4.3 `MyRestController`：REST 控制器

```java
@RestController
@RequestMapping("/api")
public class MyRestController {
    @GetMapping("/hello") public String hello() { ... }        // 只能 GET
    // 其余：@PostMapping /user、/user/{id}(@PathVariable)、/user/param(@RequestParam)、
    //       /user/body/map 与 /user/body/object(@RequestBody，后者返回 User)
}
```

| 注解 | 绑定来源 | 说明 |
| --- | --- | --- |
| `@PathVariable` | URL 路径占位符 `{}` | 默认必填，可选写 `required = false` |
| `@RequestParam` | 查询串 / 表单 | 默认必填，本模块 `name` 用了 `required = false` |
| `@RequestBody` | 请求体（按 `Content-Type` 反序列化） | 既能接 `Map` 也能接 POJO；**一个方法只能有一个** |

- 返回 `String` 由 `StringHttpMessageConverter` 写回（`text/plain;charset=UTF-8`）；返回 POJO / `Map` 由 `MappingJackson2HttpMessageConverter` 写回（`application/json`）——自动序列化成 JSON 靠的就是 `spring-boot-starter-web` 传递引入的 Jackson。

### 4.4 `UserEntity`：Lombok 与日期格式

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    private String id; private String name; private Integer age;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
```

- `@Data` 生成 getter/setter、`toString()`、`equals()`/`hashCode()`；`@Builder` 生成链式建造者；`@NoArgsConstructor`/`@AllArgsConstructor` 生成无参/全参构造（`@Builder` 需要全参，Jackson 需要无参）；
- **易踩的坑**：`@RequestBody User` 反序列化时 Jackson 要先 `new` 一个空对象再逐个 set 属性。只写 `@Builder` 而不加 `@NoArgsConstructor` 就没有默认无参构造，`/api/user/body/object` 会报 `InvalidDefinitionException: Cannot construct instance of ...`；
- **日期**：Spring Boot 默认关闭 Jackson 的 `WRITE_DATES_AS_TIMESTAMPS`，`LocalDate` 默认按 ISO-8601（`yyyy-MM-dd`）输出；要别的格式用 `@JsonFormat(pattern = "...")`，它对**序列化和反序列化都生效**。

## 5. 快速验证

启动：IDEA 里运行 [`BaseApplication`](src/main/java/com/hyao/base/BaseApplication.java) 的 `main` 方法。**同一端口同时只能跑一个实例**，被占用时会启动失败。

```bash
# 视图：redirect 是 302，forward / view 返回 index.html（也可以直接用浏览器打开）
curl -i http://localhost:8081/hello/redirect
curl -i http://localhost:8081/hello/forward
curl -i http://localhost:8081/hello/view
# REST：GET /api/hello 返回文本；下面演示 @RequestBody（返回 User）
curl -i http://localhost:8081/api/hello
curl -i -X POST http://localhost:8081/api/user/body/object \
  -H "Content-Type: application/json" \
  -d '{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-11"}'
```

| 请求 | 实测结果 |
| --- | --- |
| `GET /hello/redirect` | `302`，`Location: http://localhost:8081/index.html` |
| `GET /hello/forward`、`GET /hello/view` | `200`，响应体就是 `static/index.html` 的内容 |
| `GET /api/hello` | `200`，`Content-Type: text/plain;charset=UTF-8`，`Hello, World!` |
| `POST /api/hello` | `405`（它是 `@GetMapping`） |
| `POST /api/user` | `{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-16"}`（`birthday` 取 `LocalDate.now().plusYears(-18)`，**会随运行日期变化**） |
| `POST /api/user/0001` | `{"id":"0001","name":"hyao"}` |
| `POST /api/user/param?id=0001&name=hyao` | `{"id":"0001","name":"hyao"}` |
| `POST /api/user/body/map`、`POST /api/user/body/object` | 回显请求体 JSON，`object` 版返回 `UserEntity` |

## 6. 常见问题与踩坑

| 现象 | 原因 / 解法 |
| --- | --- |
| 接口 404 | Controller 不在启动类所在包（或其子包）下，且没配 `scanBasePackages` |
| 405 Method Not Allowed | 请求方法不匹配：`@GetMapping` 只能用 GET（如 `POST /api/hello`）；`/hello/redirect` 也限定了 GET |
| 启动报端口被占用 | 同一端口只能跑一个实例，先停掉占用端口的进程 |
| `Cannot construct instance of ...` | POJO 缺无参构造：`@Builder` 必须配 `@NoArgsConstructor` |
| 拿到的是视图名/HTML 而不是 JSON | 视图控制器返回的是视图名；要 JSON 用 `@RestController` 或加 `@ResponseBody` |
| 代码注释里的示例写着 POST | 已修正为 GET：`/api/hello` 是 `@GetMapping`，以注解为准 |
| `GET /api/hello/`（末尾多一个斜杠）返回 404 | Boot 3 / Spring 6 起默认关闭尾部斜杠匹配，路径必须与注解完全一致；`/api/hello/` 会被当成静态资源（`NoResourceFoundException`） |
| 404 返回的是 Boot 默认错误 JSON（带 `trace` 字段） | 01 不依赖 00，没有统一异常处理，也没有 `/error` 兜底；`trace`/`message` 字段是 devtools 在开发期打开的 `server.error.include-stacktrace=always` 带来的 |

## 7. 学习笔记

- **视图控制器 vs REST 控制器怎么选**：判断依据是「返回值是视图名还是响应体」——渲染页面用 `@Controller`，返回 JSON/文本 API 用 `@RestController`。
- **本模块是「裸」返回**：不做响应包装、不统一处理异常、没有 traceId。项目里这三件后续从02模块开始实现。
