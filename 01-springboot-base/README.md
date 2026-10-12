# 01-springboot-base

> ⚠️ **本 README 由 AI 自动生成**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

Spring Boot 基础入门模块。覆盖：启动类、配置文件、静态资源、以及 Spring MVC 的 **视图控制器** 与 **REST 控制器** 两套写法。

## 模块结构

```
01-springboot-base
├── pom.xml
└── src/main
    ├── java/com/hyao/base
    │   ├── BaseApplication.java               # 启动类
    │   ├── controller
    │   │   ├── MyController.java              # @Controller：返回视图名
    │   │   └── MyRestController.java          # @RestController：返回响应体
    │   └── model
    │       └── User.java                      # 实体类（Lombok + Jackson 注解）
    └── resources
        ├── application.yaml                   # 端口、视图解析器配置
        └── static/index.html                  # 静态资源
```

模块 POM 只声明了 `parent` 指向父工程，**没有写任何依赖和版本**：起步依赖由父 POM 继承，版本由父 POM 导入的 Spring Boot BOM 决定。

## 1. 启动类与 `@SpringBootApplication`

```java
@SpringBootApplication
public class BaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaseApplication.class, args);
    }
}
```

`@SpringBootApplication` 是一个组合注解，等价于下面三个注解：

| 组成注解 | 作用 |
| --- | --- |
| `@SpringBootConfiguration` | 本质是 `@Configuration`，标识这是一个配置类 |
| `@EnableAutoConfiguration` | 开启自动配置，Spring Boot 按 classpath 上的依赖自动装配 Bean（引入了 web 起步依赖，就自动配好 Tomcat + Spring MVC + Jackson） |
| `@ComponentScan` | 组件扫描，**默认扫描启动类所在包及其子包** |

最后一点很关键：启动类在 `com.hyao.base`，所以 `com.hyao.base.controller`、`com.hyao.base.model` 下的类会被自动扫描到。**如果把 Controller 写到 `com.hyao.base` 之外的包（比如 `com.other.controller`），不额外配置 `scanBasePackages` 就不会被扫描，接口会 404。**

`SpringApplication.run()` 负责创建 Spring 容器、启动内嵌 Tomcat、启动完成后阻塞主线程。

## 2. 配置文件 `application.yaml`

```yaml
# 配置端口（不配置默认8080）
server:
  port: 8080

spring:
  mvc:
    # 视图解析器配置
    view:
      prefix: /
      suffix: .html
```

| 配置项 | 作用 |
| --- | --- |
| `server.port` | 内嵌 Tomcat 端口，不配置默认 `8080` |
| `spring.mvc.view.prefix` / `suffix` | 视图解析器的前缀/后缀，见第 4 节 |

YAML 用缩进表示层级（不能用 Tab），`application.yaml` 与 `application.properties` 二选一即可，Spring Boot 都会自动加载。配置类文件可以放在 `src/main/resources` 根目录，也可以放在 `config/` 子目录下，都会被识别。

## 3. 静态资源 `static/index.html`

`src/main/resources/static/index.html` 会被 Spring Boot 当作静态资源直接对外提供，访问地址就是 <http://localhost:8080/index.html>。

Spring Boot 默认的静态资源位置（按优先级，前面的覆盖后面的）：

```
classpath:/META-INF/resources/
classpath:/resources/
classpath:/static/          ← 本项目使用的目录
classpath:/public/
```

所以静态资源 **不需要写任何 Controller**，把文件放进 `static` 目录即可通过 `http://localhost:8080/文件名` 访问。它由 Spring MVC 的 `ResourceHttpRequestHandler` 处理，不走视图解析器。

## 4. 视图控制器 `@Controller`（MyController）

### 4.1 `@Controller` 与视图解析

`@Controller` 类中的方法返回 **字符串时表示视图名**，不会直接写入响应体，而是交给视图解析器处理。

本项目 **没有引入模板引擎**（Thymeleaf 等模板技术后续再单独学），所以走的是 Spring MVC 默认的视图解析器 `InternalResourceViewResolver`，它把视图名拼接成转发路径：

```
prefix + 视图名 + suffix
```

`application.yaml` 中配置 `prefix=/`、`suffix=.html`，因此返回 `"index"` 最终会 **转发** 到 `/index.html`，也就是上面那个静态页面。如果以后引入模板引擎，这个解析器就会被模板引擎自己的视图解析器取代。

### 4.2 类级与方法级 `@RequestMapping`

```java
@Controller
@RequestMapping("/hello")
public class MyController {
    @RequestMapping(path = "/redirect", method = RequestMethod.GET)   // 完整路径 /hello/redirect
    ...
}
```

- 类上的 `@RequestMapping` 是 **公共路径前缀**；
- 方法上的 `@RequestMapping` 是该方法的路径；
- 两者 **拼接** 得到完整路径。

方法级 `@RequestMapping` 常用这两个属性：

| 属性 | 说明 |
| --- | --- |
| `path` / `value` | 请求路径，两者等价，`@RequestMapping("/forward")` 就是省略 `value` 的简写 |
| `method` | 限定的请求方法，如 `RequestMethod.GET` |

本类里两种写法都出现了：`/redirect` 显式写了 `method = RequestMethod.GET`，因此只能收到 **GET** 请求；`/forward` 和 `/view` 没写 `method`，所以 **任意 HTTP 方法**（GET/POST/PUT…）都能访问。要限定方法时，Spring 4.3 起还可以用 `@GetMapping` 这类简写（见第 5 节）。

### 4.3 重定向 vs 转发

| 写法 | 行为 | 浏览器地址 | 本质 |
| --- | --- | --- | --- |
| `return "redirect:/index.html"` | 302 重定向 | **会变化** | 服务器返回 302 + Location，由浏览器再发一次请求 |
| `return "forward:/index.html"` | 服务器内部转发 | **不变** | 在服务器内部把请求交给另一个处理器，只有一次请求 |

- 加 `redirect:` 前缀时，返回值 **不再经过视图解析器**，直接当作重定向地址。
- 加 `forward:` 前缀同理，直接当作转发地址。
- 所以 `/hello/view`、`/hello/forward` 在浏览器地址栏看着不一样，但最终渲染的都是 `static/index.html`。

### 4.4 接口一览

| 请求 | 说明 | 实测 |
| --- | --- | --- |
| `GET /hello/redirect` | 302 重定向到 `/index.html`，地址栏变为 `/index.html` | `302`，`Location: http://localhost:8080/index.html` |
| `GET /hello/forward` | 内部转发到 `/index.html`，地址栏仍是 `/hello/forward` | `200`，响应体是 index.html 的内容 |
| `GET /hello/view` | 返回视图名 `index`，经 `InternalResourceViewResolver` 解析为 `/index.html` | `200`，响应体是 index.html 的内容 |

三个路径最终渲染的都是 `static/index.html`，区别只在 HTTP 状态码和浏览器地址栏。

因为 `/redirect` 限定了 `GET`，用其他方法访问它会得到 `405`；`/forward`、`/view` 没有限定方法，就不受这个限制。

## 5. REST 控制器 `@RestController`（MyRestController）

### 5.1 `@RestController` = `@Controller` + `@ResponseBody`

`@ResponseBody` 表示方法返回值 **直接写入 HTTP 响应体**，不再当作视图名解析。`@RestController` 就是这两个注解的组合，写 REST API 时用它即可，不用每个方法再加 `@ResponseBody`。

### 5.2 请求映射注解简写

`@GetMapping("/hello")` 等价于 `@RequestMapping(value = "/hello", method = RequestMethod.GET)`。同类简写：

| 简写 | 等价于 |
| --- | --- |
| `@GetMapping` | `@RequestMapping(method = GET)` |
| `@PostMapping` | `@RequestMapping(method = POST)` |
| `@PutMapping` | `@RequestMapping(method = PUT)` |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` |
| `@PatchMapping` | `@RequestMapping(method = PATCH)` |

这里显式限定了请求方法，用错方法访问会返回 `405 Method Not Allowed`。

### 5.3 参数绑定

| 注解 | 绑定来源 | 示例 |
| --- | --- | --- |
| `@PathVariable` | URL 路径中的占位符 | `/api/user/0001` → `@PathVariable("id") String id` 得到 `0001` |
| `@RequestParam` | 请求参数（查询串或表单） | `/api/user/param?id=0001&name=hyao` |
| `@RequestBody` | 请求体，按 `Content-Type` 反序列化 | `Content-Type: application/json` + JSON 串 |

要点：

- 路径占位符用 `{}` 声明：`@PostMapping("/user/{id}")`；路径变量 **默认必填**，可选时写 `@PathVariable(value = "id", required = false)`。
- `@RequestParam` 默认也是必填，非必填要写 `@RequestParam(required = false)`（如示例中的 `name`）。用 `?id=1&name=x` 这种查询串传参，不需要 JSON。
- `@RequestBody` 既能反序列化为 `Map<String, Object>`，也能反序列化为 POJO；**一个方法只能有一个 `@RequestBody`**。
- 参数名与占位符/请求参数名一致时，注解的 `value` 可以省略，但显式写出更清晰。

### 5.4 返回值与消息转换器

方法的返回值由 `HttpMessageConverter` 决定怎么写出：

| 返回类型 | 使用的转换器 | `Content-Type` |
| --- | --- | --- |
| `String` | `StringHttpMessageConverter` | `text/plain;charset=UTF-8` |
| POJO / `Map` | `MappingJackson2HttpMessageConverter` | `application/json` |

也就是说，**返回类型既可以是 Map 也可以是 POJO**，框架都会自动序列化成 JSON。正因为 `MappingJackson2HttpMessageConverter` 存在，`@RequestBody` 能反序列化 JSON、`@RestController` 能返回 JSON——这些都来自 `spring-boot-starter-web` 传递引入的 Jackson。

### 5.5 接口一览

类上有 `@RequestMapping("/api")`，以下路径都已包含该前缀。

| 请求 | 说明 | 实测返回 |
| --- | --- | --- |
| `GET /api/hello` | 返回字符串 | `text/plain;charset=UTF-8`，`Hello, World!` |
| `POST /api/user` | 返回 `Map` | `{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-11"}` |
| `POST /api/user/{id}` | `@PathVariable` 绑定路径变量 | `/api/user/0001` → `{"id":"0001","name":"hyao"}` |
| `POST /api/user/param?id=&name=` | `@RequestParam` 绑定请求参数 | `?id=0001&name=hyao` → `{"id":"0001","name":"hyao"}`，`name` 可省略 |
| `POST /api/user/body/map` | `@RequestBody` 绑定为 `Map` | 原样回显请求体 JSON |
| `POST /api/user/body/object` | `@RequestBody` 绑定为 `User`，返回 `User` | `{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-11"}`，用 `User.builder()` 复制后返回 |

请求方法用错会得到 `405`：实测 `POST /api/hello` → `405 Method Not Allowed`。

> 代码注释里的调用示例把 `/api/hello`、`/api/user` 写成了 `POST`，实际以注解为准：`/api/hello` 是 `@GetMapping`，只能用 **GET** 访问，上面的 `405` 就是这么来的。

### 5.6 验证示例

先启动应用：在 IDEA 里运行 [`BaseApplication`](src/main/java/com/hyao/base/BaseApplication.java) 的 `main` 方法（默认端口 8080，注意同一个端口同时只能跑一个实例）。

```bash
# 视图相关（浏览器直接访问即可）
curl -i http://localhost:8080/hello/redirect
curl -i http://localhost:8080/hello/forward
curl -i http://localhost:8080/hello/view

# REST 接口
curl -i http://localhost:8080/api/hello
curl -i -X POST http://localhost:8080/api/user
curl -i -X POST http://localhost:8080/api/user/0001
curl -i -X POST "http://localhost:8080/api/user/param?id=0001&name=hyao"

curl -i -X POST http://localhost:8080/api/user/body/map \
  -H "Content-Type: application/json" \
  -d '{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-11"}'

curl -i -X POST http://localhost:8080/api/user/body/object \
  -H "Content-Type: application/json" \
  -d '{"id":"0001","name":"hyao","age":18,"birthday":"2008-10-11"}'
```

实测结果：`/hello/redirect` 返回 `302`（`Location: http://localhost:8080/index.html`），`/hello/forward` 与 `/hello/view` 返回 `200` 且响应体就是 `static/index.html`，REST 接口返回的 JSON 与 5.5 表格中一致。

## 6. 实体类与 Lombok（User）

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private Integer age;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
```

| 注解 | 作用 |
| --- | --- |
| `@Data` | 生成 getter/setter、`toString()`、`equals()`/`hashCode()` |
| `@Builder` | 生成链式建造者，如 `User.builder().id("0001").build()` |
| `@NoArgsConstructor` | 生成无参构造 |
| `@AllArgsConstructor` | 生成全参构造（`@Builder` 需要它） |

**易踩的坑：Jackson 反序列化 `@RequestBody User user` 时，必须先 `new` 一个空对象再逐个 set 属性，所以 POJO 必须有 **无参构造**。如果只写 `@Builder` 而不加 `@NoArgsConstructor`，Lombok 生成全参构造后就 **不会** 再有默认无参构造，`/api/user/body/object` 会因反序列化失败而报错（`InvalidDefinitionException: Cannot construct instance of ...`）。三个注解一起写才安全。**

### 日期序列化

- Spring Boot 默认关闭了 Jackson 的 `WRITE_DATES_AS_TIMESTAMPS`，所以 `LocalDate` 默认就按 ISO-8601（`yyyy-MM-dd`）输出，`LocalDateTime` 会输出 `yyyy-MM-ddTHH:mm:ss`。实测 `POST /api/user` 返回的 `birthday` 就是 `"2008-10-11"`——这里的 `LocalDate` 是放在 `Map` 里的、并没有加 `@JsonFormat`，靠的就是这个默认配置。
- 需要别的格式时用 `@JsonFormat(pattern = "...")` 指定，如 `User.birthday` 上的 `@JsonFormat(pattern = "yyyy-MM-dd")`；它同时作用于 **序列化（返回 JSON）和反序列化（接收 JSON）**。
- 也可以全局配置：`spring.jackson.date-format` / `spring.jackson.time-zone`（对 `java.util.Date` 生效），或自定义 `Jackson2ObjectMapperBuilderCustomizer`。

## 7. devtools 热部署

`spring-boot-devtools` 已在父 POM 中声明（`runtime` + `optional`）。它的作用是：

- 监听 classpath 下文件变化，编译后 **自动重启** 应用上下文（比手动停止再启动快，但仍然是重启，静态资源和部分第三方 jar 除外）；
- 修改 `static/` 下的静态资源、模板文件通常无需重启；
- 修改 Java 代码后需要触发一次编译（IDEA 中 `Ctrl + F9` 或开启 `Build project automatically`）。

## 8. 小结

| 知识点 | 位置 |
| --- | --- |
| `@SpringBootApplication` 与包扫描 | `BaseApplication` |
| `server.port`、视图解析器配置 | `application.yaml` |
| 静态资源目录 | `resources/static/index.html` |
| `@Controller` 返回视图名、`redirect:` / `forward:` | `MyController` |
| `@RestController`、`@GetMapping`/`@PostMapping` | `MyRestController` |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | `MyRestController` |
| 消息转换器与 JSON 序列化 | `MyRestController` + Jackson |
| Lombok、`@JsonFormat`、无参构造的坑 | `User` |
| 热部署 | `spring-boot-devtools` |
