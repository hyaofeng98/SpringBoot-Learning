# 00-springboot-common

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 1. 学习目标

本模块是**公共库**（没有启动类，产物是普通 jar），把三项横切能力做成 **starter 式自动配置**，使用方加一行依赖即生效：**统一响应包装**（返回值包成 `Result<T>`）、**全局异常处理**（业务异常 200 + 业务码，框架异常保留真实状态码）、**统一日志（基础版）**（MDC 把 `traceId` 贯穿一次请求，并给使用方一套默认 Logback 配置）。顺带掌握 **Starter 三件套**（`AutoConfiguration.imports` 注册入口、`@AutoConfiguration` 控制顺序、`@ConditionalOn*` 控制条件与可覆盖），以及「错误码接口 + 实现两层、系统码归库、业务码归模块」的设计。

## 2. 学习清单

| 状态 | 优先级 | 知识点 | 说明 |
| --- | --- | --- | --- |
| ✅ | — | **统一响应包装** | `Result<T>` 统一响应体（成功码 `SUC0000`；`ok()`/`fail()` 工厂；`fail(code, message)` 显式传参、脱敏）+ `ResponseWrapperAdvice` 自动包装（`supports()` 判定顺序、`String` 必须手动序列化）+ `@IgnoreWrapper` 方法级/类级跳过 |
| ✅ | — | **自定义业务异常** | `IErrorCode` 错误码契约 + `SysErrorCode` 系统码（库里只放 `SYS0001`/`SYS0002`，业务码由业务模块实现接口扩展）+ `BizException`（面向 `IErrorCode` 编程、`MessageFormat` 填 `{0}`）+ `GlobalExceptionHandler`（继承 `ResponseEntityExceptionHandler`，重写 `handleExceptionInternal` 保留真实状态码） |
| ✅ | — | **统一日志** | `TraceIdFilter` + `TraceIdUtils`：MDC 存 traceId、响应头回写、`finally` 清理；默认 `logback-spring.xml`：控制台按级别着色（`%clr`）+ 文件/错误文件滚动 + 多环境 |
| ✅ | — | **Starter 式自动配置** | `AutoConfiguration.imports` 注册入口 + `@AutoConfiguration(after = JacksonAutoConfiguration)` 控制顺序 + `@ConditionalOnWebApplication` / `@ConditionalOnMissingBean` 控制条件，三个 Bean 都可被使用方覆盖 |
| ⬜ | ★★☆ | `traceId` 跨线程传递 | MDC 底层是 ThreadLocal，`@Async` / 线程池 / MQ 里会丢 |

## 3. 依赖与配置

- **依赖**：POM **不额外声明依赖**——`spring-boot-starter-web`（Spring MVC + Jackson + `spring-boot-autoconfigure` + Servlet API）、`commons-lang3`（`TraceIdFilter` 用 `StringUtils`）、`lombok` 都继承自父 POM 的公共依赖；
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
public enum SysErrorCode implements IErrorCode { SYSTEM_ERROR("SYS0001","系统内部异常"); REQUEST_ERROR("SYS0002","请求异常"); }
public BizException(IErrorCode errorCode, Object... message) {             // 业务码由业务模块提供
    this.code = errorCode.getCode(); this.message = MessageFormat.format(errorCode.getMessage(), message);
}
```

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

### 4.5 `GlobalExceptionHandler`：全局异常处理

```java
@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BizException.class)  @ExceptionHandler(Exception.class)   // 业务异常 + 未知异常兜底
    @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest req) {
        SysErrorCode code = status.is4xxClientError() ? REQUEST_ERROR : SYSTEM_ERROR;  // 4xx→SYS0002，5xx→SYS0001
        return new ResponseEntity<>(Result.fail(code.getCode(), code.getMessage()), headers, status);
    }
}
```

| 方法 | 处理什么 | HTTP 状态码 | code |
| --- | --- | --- | --- |
| `handleException(BizException)` | 业务异常 | `200` | 业务码（如 `QRY0001`） |
| `handleExceptionInternal(...)` | 框架异常（405/400/415…） | 保留原始状态码（`405` 带 `Allow`） | 4xx → `SYS0002`；5xx → `SYS0001` |
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
@AutoConfiguration(after = JacksonAutoConfiguration.class)   // 排在 Jackson 之后：要注入它的 ObjectMapper
@ConditionalOnWebApplication(type = SERVLET) @ConditionalOnClass({ResponseBodyAdvice.class, ObjectMapper.class})
public class CommonWebAutoConfiguration {
    @Bean @ConditionalOnMissingBean ResponseWrapperAdvice responseWrapperAdvice(ObjectMapper om) { … }
    @Bean @ConditionalOnMissingBean GlobalExceptionHandler globalExceptionHandler() { … }
    @Bean @ConditionalOnMissingBean TraceIdFilter traceIdFilter() { … }
}
```

- `AutoConfiguration.imports` 决定「怎么被找到」，`@AutoConfiguration` 决定「什么时候生效」，`@ConditionalOn*` 决定「什么条件生效、能否被覆盖」；
- `@Bean` 注册的 `@ControllerAdvice` 一样生效：Spring MVC 是按**容器里 Bean 的类型**找 advice，不走包扫描。使用方加一行依赖即可（版本由父 POM 的 `dependencyManagement` 管）：`com.hyao.common:00-springboot-common`。

## 5. 快速验证

本模块**是库、没有启动类**，遂单独实现02模块验证。在 IDEA 里运行 02 的 `AdviceApplication`（端口 `8082`）后：

```bash
curl -i http://localhost:8082/api/hello            # String 也会被包装
curl -i -X POST http://localhost:8082/error/user   # 业务异常
curl -i -X POST http://localhost:8082/api/hello    # 方法不支持（该接口只有 @GetMapping）
```

| 请求 | 预期结果 |
| --- | --- |
| `GET /api/hello` | `200` + `{"code":"SUC0000","message":"操作成功","data":"Hello World!"}` |
| `POST /error/user` | `200` + `{"code":"QRY0001","message":"查询错误: 查询用户为空！","data":null}`（`QRY0001` 与文案来自 02 的 `BizErrorCode`） |
| `POST /api/hello` | `405` + 响应头 `Allow: GET`，body `{"code":"SYS0002","message":"请求异常","data":null}` |
| 任意请求 | 响应头带 `traceId`；`logs/app.log`（相对应用工作目录）里的请求日志也是同一个 `traceId` |

只想校验「库能被依赖、消费方能编译」时：`mvn -pl 00-springboot-common install -DskipTests`，再 `mvn -pl 02-springboot-advice -am test-compile`。

## 6. 常见问题与踩坑

1. **控制台日志没有级别颜色**：颜色来自 pattern 里的 `%clr(...)`（Boot 的 `ColorConverter`，不指定颜色时按级别取色），而 `%clr` 是 Boot 注册的转换词，**必须先 `<include resource="org/springframework/boot/logging/logback/defaults.xml"/>`**。本模块已按「CONSOLE 带 `%clr`、FILE/ERROR_FILE 纯文本」分开配置；漏了 include 会没颜色，把带 `%clr` 的 pattern 共用给文件 appender 会把 ANSI 转义序列写进日志文件。
2. **`@Bean` 注册的 `@ControllerAdvice` 为什么也生效**：Spring MVC 按容器里 Bean 的**类型**查找 `@ControllerAdvice`，不走包扫描。
3. **业务异常文案与脱敏**：`new BizException(errorCode, args...)` 由 `MessageFormat` 填充，message 里没有 `{0}` 时参数会被忽略（02 的 `QUERY_ERROR` 特意写成 `"查询错误: {0}"` 来演示）；`Result.fail` 只收 `code`/`message`，是为了不把异常原文回传客户端。
4. **库不能打成可执行 jar**：那样类会进 `BOOT-INF/classes`，其他模块编译期找不到；本模块没有任何打包插件配置，产物就是普通 jar。
5. **兜底的 `@ExceptionHandler(Exception.class)` 会接住框架异常**：想让某类异常（如 `DataAccessException`）走自己的逻辑，就单独加一个 advice、用**类级 `@Order`** 排到兜底前面（03 的做法）。

## 7. 学习笔记

- **Starter 三件套分工**：`AutoConfiguration.imports` 管「怎么被找到」、`@AutoConfiguration(after = …)` 管「什么时候生效」、`@ConditionalOn*` 管「什么条件生效、能否被覆盖」；`after = JacksonAutoConfiguration.class` 是为了能注入 Boot 自动配置的 `ObjectMapper`。
- **两个扩展点**：`ResponseBodyAdvice` 在 `@ResponseBody` 返回值被 `HttpMessageConverter` 写出响应体**之前**介入（统一包装的标准做法）；`ResponseEntityExceptionHandler` 让框架异常都汇聚到 `handleExceptionInternal`，重写一处即可统一响应体，同时保住状态码和 `Allow` 这类响应头。
- **「业务异常 200 + 业务码」是项目约定**：业务失败靠 `code` 区分，请求/框架层面的错误保留真实 HTTP 状态码，两者共用同一个 `Result` 结构。
- 建议阅读顺序：`Result` → `IErrorCode`/`SysErrorCode`/`BizException` → 两个 advice → 自动配置；用法演示见 [02 模块 README](../02-springboot-advice/README.md)。
