# 00-springboot-common

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 1. 学习目标

本模块是**公共库**（没有启动类，产物是普通 jar），把**四项横切能力**做成 **starter 式自动配置**，使用方加一行依赖即生效：**统一响应包装**（返回值包成 `Result<T>`）、**全局异常处理**（业务异常 200 + 业务码，请求/参数/框架异常保留真实状态码）、**参数校验**（Bean Validation 校验失败统一成 `400` + `SYS0004`/`SYS0005`）、**统一日志（基础版）**（MDC 把 `traceId` 贯穿一次请求，并给使用方一套默认 Logback 配置）。顺带掌握 **Starter 三件套**（`AutoConfiguration.imports` 注册入口、`@AutoConfiguration` 控制顺序、`@ConditionalOn*` 控制条件与可覆盖）、`/error` 兜底（404 等不进 advice 的错误也统一成 `Result`），以及「错误码接口 + 实现两层、系统码归库、业务码归模块」的设计。

## 2. 学习清单

| 状态 | 优先级 | 知识点 | 说明 |
| --- | --- | --- | --- |
| ✅ | — | **统一响应包装** | `Result<T>` 统一响应体（成功码 `SUC0000`；`ok()`/`fail()` 工厂；`fail(code, message)` 显式传参、脱敏）+ `ResponseWrapperAdvice` 自动包装（`supports()` 判定顺序、`String` 必须手动序列化）+ `@IgnoreWrapper` 方法级/类级跳过 |
| ✅ | — | **自定义业务异常** | `IErrorCode` 错误码契约 + `SysErrorCode` 系统码（`SYS0001` 系统异常、`SYS0002` 方法不支持、`SYS0003` 接口不存在、`SYS0004` 参数校验失败、`SYS0005` 参数类型不匹配、`SYS0006` 其余 4xx 兜底；业务码由业务模块实现接口扩展）+ `BizException`（面向 `IErrorCode` 编程、`MessageFormat` 填 `{0}`）+ `GlobalExceptionHandler`（继承 `ResponseEntityExceptionHandler`，按异常类型逐个处理并保留真实状态码） |
| ✅ | — | **参数校验（Bean Validation）** | `spring-boot-starter-validation` + `@Valid @RequestBody` / 类上 `@Validated`；`MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException`、`MethodArgumentTypeMismatchException`、参数缺失、请求体解析失败 → `400` + `SYS0004`/`SYS0005`，字段错误拼成 `字段: 提示` |
| ✅ | — | **`/error` 兜底统一** | `CommonErrorController`：404（URL 找不到）、容器层 `sendError` 等**不进 `@ControllerAdvice`** 的错误也返回 `Result`（`SYS0003`/`SYS0002`/`SYS0004`），并让 Boot 自带的 `BasicErrorController` 退让 |
| ✅ | — | **统一日志** | `TraceIdFilter` + `TraceIdUtils`：MDC 存 traceId、响应头回写、`finally` 清理；默认 `logback-spring.xml`：控制台按级别着色（`%clr`）+ 文件/错误文件滚动 + 多环境 |
| ✅ | — | **Starter 式自动配置** | `AutoConfiguration.imports` 注册入口 + `@AutoConfiguration(after = JacksonAutoConfiguration, before = ErrorMvcAutoConfiguration)` 控制顺序 + `@ConditionalOnWebApplication` / `@ConditionalOnMissingBean` 控制条件，四个 Bean（包装 advice / 全局异常 / error 控制器 / traceId 过滤器）都可被使用方覆盖 |
| ⬜ | ★★☆ | `traceId` 跨线程传递 | MDC 底层是 ThreadLocal，`@Async` / 线程池 / MQ 里会丢 |

## 3. 依赖与配置

- **依赖**：POM 只额外声明 `spring-boot-starter-validation`（Bean Validation / Hibernate Validator）；`spring-boot-starter-web`（Spring MVC + Jackson + `spring-boot-autoconfigure` + Servlet API）、`commons-lang3`（`TraceIdFilter` 用 `StringUtils`）、`lombok` 仍继承自父 POM 的公共依赖；
- **自动配置入口** `META-INF/spring/…AutoConfiguration.imports`：只有一行 `com.hyao.common.autoconfigure.CommonWebAutoConfiguration`；
- **默认日志配置** `logback-spring.xml`：格式 `%d{…} [%thread] %-5level [%X{traceId:-}] %logger{40} - %msg%n`——`%X{traceId:-}` 从 MDC 取值（取不到显示 `-`），`%clr(%-5level)` 不指定颜色时**按级别取色**；带 `%clr` 的 pattern **只给 CONSOLE**，文件用纯文本（否则日志文件里会写进 ANSI 转义序列）；appender 为 CONSOLE / FILE（按天+大小滚动，30 天、5GB）/ ERROR_FILE（`LevelFilter` 只收 ERROR，60 天），`APP_NAME` 取自 `spring.application.name`（默认 `app`）；
- 多环境：`dev`（控制台+文件）、`prod`（文件+错误文件）、`!dev & !prod`（默认）；`org.springframework`/`org.mybatis`/`com.zaxxer.hikari` 降噪到 WARN；使用方自己的 `logback-spring.xml` 优先。

## 4. 核心实现

### 4.1 `Result`：统一响应体

```java
public static <T> Result<T> ok(T data) { return build("SUC0000", "操作成功", data); }
public static <T> Result<T> fail()     { return fail(SysErrorCode.SYSTEM_ERROR.getCode(), SysErrorCode.SYSTEM_ERROR.getMessage()); }
public static <T> Result<T> fail(String code, String message) { return build(code, message, null); }   // 只收 code/message，不收异常对象
```

### 4.2 `IErrorCode` / `SysErrorCode` / `BizException`：错误码两层设计

```java
public interface IErrorCode { String getCode(); String getMessage(); }
public enum SysErrorCode implements IErrorCode {
    SYSTEM_ERROR("SYS0001", "系统内部异常"),
    METHOD_NOT_SUPPORTED("SYS0002", "请求方法不支持"),      // 405
    HANDLER_NOT_FOUND("SYS0003", "接口不存在"),             // 404
    PARAM_INVALID("SYS0004", "参数校验失败"),               // 400
    PARAM_TYPE_MISMATCH("SYS0005", "参数类型不匹配"),        // 400
    REQUEST_ERROR("SYS0006", "请求异常");                   // 其余 4xx 兜底
}
public BizException(IErrorCode errorCode, Object... message) {             // 业务码由业务模块提供
    this.code = errorCode.getCode(); this.message = MessageFormat.format(errorCode.getMessage(), message);
}
```

| 错误码 | 含义 | 典型场景 | HTTP 状态码 |
| --- | --- | --- | --- |
| `SYS0001` | 系统内部异常 | 未预期异常、5xx | `500`（未知异常兜底时为 `200`） |
| `SYS0002` | 请求方法不支持 | 接口只接受 POST，用了 GET | `405`（带 `Allow` 头） |
| `SYS0003` | 接口不存在 | URL 拼错、没有匹配的 Handler | `404` |
| `SYS0004` | 参数校验失败 | `@Valid` 校验不通过、缺必填参数、请求体解析失败 | `400` |
| `SYS0005` | 参数类型不匹配 | `pageSize=abc` 这类类型转换失败 | `400` |
| `SYS0006` | 请求异常 | 其余 4xx（如 415） | 原始 4xx |

业务模块实现 `IErrorCode` 即可扩展错误码（如 02 的 `BizErrorCode`：`QRY0001`、`FDL0001`），公共库不需要知道有哪些业务码；`args` 靠 `MessageFormat` 填充，message 里没有 `{0}` 时参数会被忽略。

### 4.3 `ResponseWrapperAdvice`：统一响应包装

```java
@IgnoreWrapper 在方法/类上 → false；返回类型是 Result → false；返回类型是 ResponseEntity → false
返回类型是 String → true（走特殊处理）；其余：转换器是 MappingJackson2HttpMessageConverter 才包装

if (body instanceof Result) return body;
if (returnType.getParameterType() == String.class) {
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return objectMapper.writeValueAsString(Result.ok(body));   // String 必须手动序列化
}
return Result.ok(body);
```

- `String` 那条判定必须放在 Jackson 判定**之前**：`String` 用的转换器是 `StringHttpMessageConverter`，按 Jackson 那条会被排除；前三条是「排除项」，必须放最前；
- **`String` 为什么要特殊处理**：`StringHttpMessageConverter` 会把返回值直接当字符串写出（相当于 `(String) body` 强转），直接返回 `Result` 会抛 `ClassCastException`，所以要手动序列化（用的是 Boot 自动配置的那个 `ObjectMapper`，全局 Jackson 配置同样生效）再改 `Content-Type`。

### 4.4 `IgnoreWrapper`：跳过包装

`@Target({METHOD, TYPE})` + `@Retention(RUNTIME)` 的空注解：`RUNTIME` 必须保留，因为 `supports()` 是用反射（`hasMethodAnnotation` / `isAnnotationPresent`）读它的；标在方法上跳过单个接口，标在类上跳过整个 Controller。

### 4.5 `GlobalExceptionHandler`：全局异常处理 + 参数校验

```java
@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BizException.class) @Order(Ordered.HIGHEST_PRECEDENCE)
    public Result<Void> handleException(BizException be) {                   // 业务异常：200 + 业务码
        return Result.fail(be.getCode(), be.getMessage());
    }

    // 框架异常：逐个覆盖基类的钩子，保留状态码，只换 body
    @Override protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(...)  // 405 → SYS0002（带支持的方法）
    @Override protected ResponseEntity<Object> handleNoHandlerFoundException(...)        // 404 → SYS0003
    @Override protected ResponseEntity<Object> handleMethodArgumentNotValid(...)         // 400 → SYS0004（@Valid @RequestBody）
    @Override protected ResponseEntity<Object> handleBindException(...)                  // 400 → SYS0004（表单/查询对象）
    @Override protected ResponseEntity<Object> handleMissingServletRequestParameter(...) // 400 → SYS0004（缺参数）
    @Override protected ResponseEntity<Object> handleHttpMessageNotReadable(...)         // 400 → SYS0004（JSON 解析失败）
    @ExceptionHandler(ConstraintViolationException.class)                                // 400 → SYS0004（@Validated + @RequestParam）
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)                         // 400 → SYS0005（类型不匹配）

    @Override protected ResponseEntity<Object> handleExceptionInternal(...) {
        SysErrorCode code = status.is4xxClientError() ? REQUEST_ERROR : SYSTEM_ERROR;     // 其余 4xx→SYS0006，5xx→SYS0001
        return new ResponseEntity<>(Result.fail(code.getCode(), code.getMessage()), headers, status);
    }

    @ExceptionHandler(Exception.class) @Order
    public Result<Void> handleException(Exception e) { return Result.fail(); }            // 未知异常：200 + SYS0001
}
```

| 方法 | 处理什么 | HTTP 状态码 | code |
| --- | --- | --- | --- |
| `handleException(BizException)` | 业务异常 | `200` | 业务码（如 `QRY0001`） |
| `handleHttpRequestMethodNotSupported` | 请求方法不支持 | `405`（带 `Allow`） | `SYS0002`，message 里带「该接口仅支持 GET」 |
| `handleNoHandlerFoundException` | URL 找不到 Handler（需开启 `spring.mvc.throw-exception-if-no-handler-found`） | `404` | `SYS0003` |
| `handleMethodArgumentNotValid` | `@Valid @RequestBody` 校验失败 | `400` | `SYS0004` + `字段: 提示` |
| `handleBindException` | 表单 / 查询参数绑定对象后校验失败 | `400` | `SYS0004` |
| `handleMissingServletRequestParameter` | 缺少必填参数 | `400` | `SYS0004` + 参数名 |
| `handleHttpMessageNotReadable` | 请求体不是合法 JSON / 字段类型不匹配 | `400` | `SYS0004` |
| `handleConstraintViolation` | 类上 `@Validated` 时 `@RequestParam`/`@PathVariable` 校验失败 | `400` | `SYS0004` |
| `handleMethodArgumentTypeMismatch` | 参数类型转换失败（如 `pageSize=abc`） | `400` | `SYS0005` |
| `handleExceptionInternal(...)` | 其余框架异常（415 等） | 保留原始状态码 | 4xx → `SYS0006`；5xx → `SYS0001` |
| `handleException(Exception)` | 未知异常 | `200` | `SYS0001` |

- `handleExceptionInternal` 是基类所有内置处理的**汇聚点**，重写它就能在保留状态码/响应头的前提下统一 body；返回类型必须是 `ResponseEntity<Object>`（基类签名如此，泛型不协变）；
- 同一个 advice 内按「异常类型最具体优先」匹配（`BizException` 命中业务方法，其余落到兜底）；**跨 advice 的顺序由 advice Bean 上的 `@Order` 决定**——03 的 `DbExceptionHandler` 用类级 `@Order(HIGHEST_PRECEDENCE)` 抢在兜底之前，把 `DataAccessException` 变成 `DBE0001`；4xx 记 `WARN`、5xx 记 `ERROR`（带堆栈），细节只进日志。

### 4.6 `TraceIdFilter` / `TraceIdUtils`：统一日志

```java
@Order(Ordered.HIGHEST_PRECEDENCE) public class TraceIdFilter implements Filter {
    String traceId = req.getHeader(TRACE_ID);                             // 优先复用上游传入的
    if (StringUtils.isBlank(traceId)) traceId = TraceIdUtils.generate(); // 否则生成（UUID 去横线，32 位）
    TraceIdUtils.setTraceId(traceId); resp.setHeader(TRACE_ID, traceId);  // 存 MDC + 回写响应头
    try { log.info("请求开始 {} {}", method, uri); chain.doFilter(request, response); }
    finally { log.info("请求结束 … 耗时={}ms", …); TraceIdUtils.clear(); }  // 必须清理
}
```

`finally` 里 `clear()` 不能省：Servlet 线程池化复用，不清理会让下一个请求的日志带上上一个请求的 `traceId`（串号）。类上**没有** `@Component`，由自动配置注册，避免使用方扫描到它时出现两个实例。

### 4.7 `CommonWebAutoConfiguration`：Starter 式自动配置

```java
@AutoConfiguration(after = JacksonAutoConfiguration.class, before = ErrorMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = SERVLET) @ConditionalOnClass({ResponseBodyAdvice.class, ObjectMapper.class})
public class CommonWebAutoConfiguration {
    @Bean @ConditionalOnMissingBean ResponseWrapperAdvice responseWrapperAdvice(ObjectMapper om) { … }
    @Bean @ConditionalOnMissingBean GlobalExceptionHandler globalExceptionHandler() { … }
    @Bean @ConditionalOnMissingBean(ErrorController.class) CommonErrorController commonErrorController() { … }
    @Bean @ConditionalOnMissingBean TraceIdFilter traceIdFilter() { … }
}
```

- `after = JacksonAutoConfiguration.class` 是为了能注入 Boot 自动配置的 `ObjectMapper`；`before = ErrorMvcAutoConfiguration.class` 是为了让 `CommonErrorController` 抢在 `BasicErrorController` 之前注册；
- `AutoConfiguration.imports` 决定「怎么被找到」，`@AutoConfiguration` 决定「什么时候生效」，`@ConditionalOn*` 决定「什么条件生效、能否被覆盖」；
- `@Bean` 注册的 `@ControllerAdvice` 一样生效：Spring MVC 是按**容器里 Bean 的类型**找 advice，不走包扫描。使用方加一行依赖即可（版本由父 POM 的 `dependencyManagement` 管）：`com.hyao.common:00-springboot-common`。

### 4.8 `CommonErrorController`：`/error` 也统一成 `Result`

有些错误**不会进入 `@ControllerAdvice`**：URL 没匹配到任何 Handler（404）、过滤器/容器层直接 `sendError`、响应已提交后的错误。Boot 默认把它们交给 `BasicErrorController`（返回 Boot 自己的 JSON 结构），和项目的 `Result` 不一致。

```java
@RestController
public class CommonErrorController implements ErrorController {
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<Result<Void>> error(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);                 // 从 ERROR_STATUS_CODE 取
        SysErrorCode code = resolveErrorCode(status);               // 404→SYS0003、405→SYS0002、400→SYS0004、其余 4xx→SYS0006
        return ResponseEntity.status(status).body(Result.fail(code.getCode(), message));
    }
}
```

- 注册到容器后，Boot 的 `BasicErrorController` 因 `@ConditionalOnMissingBean(ErrorController.class)` 自动退让；
- 自动配置类上必须加 `@AutoConfiguration(before = ErrorMvcAutoConfiguration.class)`，保证本 Bean **先**注册（顺序反了的话 `BasicErrorController` 会先占位）；
- 404 的 message 会带上请求路径（`接口不存在: /nope`），方便定位。

## 5. 快速验证

本模块**是库、没有启动类**，遂单独实现02模块验证。在 IDEA 里运行 02 的 `AdviceApplication`（端口 `8082`）后：

```bash
curl -i http://localhost:8082/api/hello            # String 也会被包装
curl -i -X POST http://localhost:8082/error/user   # 业务异常（200 + 业务码）
curl -i -X POST http://localhost:8082/api/hello    # 405：方法不支持
curl -i http://localhost:8082/nope                 # 404：接口不存在
curl -i -X POST http://localhost:8082/api/user/valid \
  -H 'Content-Type: application/json' -d '{"id":"0001","age":18}'   # 400：name 不能为空
```

| 请求 | 实测结果 |
| --- | --- |
| `GET /api/hello` | `200` + `{"code":"SUC0000","message":"操作成功","data":"Hello World!"}` |
| `POST /error/user` | `200` + `{"code":"QRY0001","message":"查询错误: 查询用户为空！","data":null}`（`QRY0001` 与文案来自 02 的 `BizErrorCode`） |
| `POST /api/hello` | `405` + 响应头 `Allow: GET` + `{"code":"SYS0002","message":"请求方法不支持，该接口仅支持 GET","data":null}` |
| `GET /nope` | `404` + `{"code":"SYS0003","message":"接口不存在: /nope","data":null}` |
| `POST /api/user/valid`，body `{"id":"0001","age":18}` | `400` + `{"code":"SYS0004","message":"参数校验失败: name: 不能为空","data":null}` |
| `POST /api/user/valid`，body `{"name":"12345678901"}` | `400` + `{"code":"SYS0004","message":"参数校验失败: name: 长度不能超过 10","data":null}` |
| 请求体不是合法 JSON | `400` + `{"code":"SYS0004","message":"参数校验失败，请求体解析失败","data":null}` |
| 任意请求 | 响应头带 `traceId`；`logs/app.log`（相对应用工作目录）里的请求日志也是同一个 `traceId` |

只想校验「库能被依赖、消费方能编译」时：`mvn -pl 00-springboot-common install -DskipTests`，再 `mvn -pl 02-springboot-advice -am test-compile`。

## 6. 常见问题与踩坑

1. **控制台日志没有级别颜色**：颜色来自 pattern 里的 `%clr(...)`（Boot 的 `ColorConverter`，不指定颜色时按级别取色），而 `%clr` 是 Boot 注册的转换词，**必须先 `<include resource="org/springframework/boot/logging/logback/defaults.xml"/>`**。本模块已按「CONSOLE 带 `%clr`、FILE/ERROR_FILE 纯文本」分开配置；漏了 include 会没颜色，把带 `%clr` 的 pattern 共用给文件 appender 会把 ANSI 转义序列写进日志文件。
2. **`@Bean` 注册的 `@ControllerAdvice` 为什么也生效**：Spring MVC 按容器里 Bean 的**类型**查找 `@ControllerAdvice`，不走包扫描。
3. **业务异常文案与脱敏**：`new BizException(errorCode, args...)` 由 `MessageFormat` 填充，message 里没有 `{0}` 时参数会被忽略（02 的 `QUERY_ERROR` 特意写成 `"查询错误: {0}"` 来演示）；`Result.fail` 只收 `code`/`message`，是为了不把异常原文回传客户端。
4. **库不能打成可执行 jar**：那样类会进 `BOOT-INF/classes`，其他模块编译期找不到；本模块没有任何打包插件配置，产物就是普通 jar。
5. **兜底的 `@ExceptionHandler(Exception.class)` 会接住框架异常**：想让某类异常（如 `DataAccessException`）走自己的逻辑，就单独加一个 advice、用**类级 `@Order`** 排到兜底前面（03 的做法）。
6. **校验为什么不生效**：约束注解只有被「触发」才生效——`@RequestBody` 参数要标 `@Valid`；`@RequestParam` / `@PathVariable` 上的约束需要**类上**标 `@Validated`（否则静默不校验）；嵌套对象要再加一层 `@Valid`。Boot 2.7 用的是 `javax.validation`，Boot 3 才是 `jakarta.validation`，别混用。
7. **404 有两种处理路径**：开启 `spring.mvc.throw-exception-if-no-handler-found=true` 时走 `handleNoHandlerFoundException`；否则由容器的 `/error` 兜底，也就是 `CommonErrorController`。两条路径的 code 都是 `SYS0003`。

## 7. 学习笔记

- **Starter 三件套分工**：`AutoConfiguration.imports` 管「怎么被找到」、`@AutoConfiguration(after = …)` 管「什么时候生效」、`@ConditionalOn*` 管「什么条件生效、能否被覆盖」；`after = JacksonAutoConfiguration.class` 是为了能注入 Boot 自动配置的 `ObjectMapper`。
- **两个扩展点**：`ResponseBodyAdvice` 在 `@ResponseBody` 返回值被 `HttpMessageConverter` 写出响应体**之前**介入（统一包装的标准做法）；`ResponseEntityExceptionHandler` 让框架异常都汇聚到 `handleExceptionInternal`，重写一处即可统一响应体，同时保住状态码和 `Allow` 这类响应头。
- **「业务异常 200 + 业务码」是项目约定**：业务失败靠 `code` 区分；请求/参数层面的错误（405/404/400）保留真实 HTTP 状态码，但 body 同样是 `Result`。参数校验失败统一 `400 + SYS0004` 而不是 200，是为了让调用方 / 网关 / 日志能直接按 HTTP 状态码区分「请求本身有问题」。
- 建议阅读顺序：`Result` → `IErrorCode`/`SysErrorCode`/`BizException` → 两个 advice → 自动配置；用法演示见 [02 模块 README](../02-springboot-advice/README.md)。
