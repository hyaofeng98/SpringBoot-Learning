# 02-springboot-advice

> ⚠️ **本 README 由 AI 自动生成**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 迁移说明：三项能力的实现已抽到公共库

> 这一节说明**为什么现在在本模块里已经找不到这三项能力的实现**。下面正文保留的是**抽取之前**的形态——知识点、类名、示例输出都不变，只是包名和代码位置变了。

**事由**：这三项能力最早就直接写在本模块里。后面的模块（03、04…）也使用同一套约定，为了避免后续子模块写重复代码，特地抽到公共库。

**做法**：把这三项能力整体搬到公共库 [00-springboot-common](../00-springboot-common/README.md)，并做成 **starter 式自动配置**——依赖方加一行依赖就生效，不需要写 `scanBasePackages`。本模块则退化为**示例模块**，只保留 Controller、实体和配置，用来演示这些能力。

| 迁移项 | 迁移前（本模块） | 迁移后（[00 模块](../00-springboot-common/README.md)） |
| --- | --- | --- |
| 根包名 | `com.hyao.advice` | `com.hyao.common` |
| 统一响应体 / 错误码 | `com.hyao.advice.common.Result` / `ErrorCode` | `com.hyao.common.result.Result` / `IErrorCode` / `ErrorCode` |
| 业务异常 / 全局异常处理 | `com.hyao.advice.common.BizException` / `advice.exception.GlobalExceptionHandler` | `com.hyao.common.exception.BizException` / `GlobalExceptionHandler` |
| 响应包装 / 跳过包装注解 | `com.hyao.advice.advice.ResponseWrapperAdvice` / `IgnoreWrapper` | `com.hyao.common.advice.ResponseWrapperAdvice` / `IgnoreWrapper` |
| traceId 过滤器 / 工具类 / 日志配置 | `com.hyao.advice.log.TraceIdFilter`、`TraceIdUtils`、`logback-spring.xml` | `com.hyao.common.log.*`、`logback-spring.xml` |
| 生效方式 | 靠包扫描（`@Component`、同包 `@ControllerAdvice`） | `AutoConfiguration.imports` 自动配置 |
| 本模块的 POM | 无额外依赖 | 依赖 `00-springboot-common` |

## 全局异常处理 + 统一响应包装 + 统一日志
本模块做三件「全局收口」的事，让业务代码里看不到包装、try-catch 和 traceId 处理：

| 能力 | 实现 | 用到的机制 |
| --- | --- | --- |
| 统一响应包装：把接口返回值统一包成 `Result<T>` | `ResponseWrapperAdvice` | `@ControllerAdvice` + `ResponseBodyAdvice` |
| 全局异常处理：把异常统一转成 `Result` | `GlobalExceptionHandler` | `@RestControllerAdvice` + `@ExceptionHandler` + `ResponseEntityExceptionHandler` |
| 统一日志（基础版）：traceId 贯穿 + 统一格式 | `TraceIdFilter`、`TraceIdUtils`、`logback-spring.xml` | `Filter` + `MDC` + Logback |

> 📌 **本模块的示例只供学习**：三段逻辑都是「能跑通的最小实现」，真实项目要**具体场景具体分析**（比如响应要不要统一包、包哪些接口、异常要不要保留 HTTP 状态码、日志怎么落盘和采集）。
>
> 这里的**统一日志很基础**——只做了 traceId 贯穿和一份基础的 Logback 配置。**生产级别的统一日志**（链路追踪、日志采集与检索、结构化输出、敏感信息脱敏、异步落盘等）列为后续计划单独学习。

## 模块结构

```
02-springboot-advice
├── pom.xml
└── src/main
    ├── java/com/hyao/advice
    │   ├── AdviceApplication.java              # 启动类
    │   ├── advice
    │   │   ├── IgnoreWrapper.java              # 自定义注解：跳过包装
    │   │   └── ResponseWrapperAdvice.java      # 响应包装（ResponseBodyAdvice）
    │   ├── common
    │   │   ├── BizException.java               # 业务异常
    │   │   ├── ErrorCode.java                  # 错误码枚举
    │   │   └── Result.java                     # 统一响应体
    │   ├── controller
    │   │   ├── DownloadController.java         # 文件下载（跳过包装）
    │   │   ├── ErrorController.java            # 抛业务异常
    │   │   └── UserController.java             # 正常返回的四种写法
    │   ├── exception
    │   │   └── GlobalExceptionHandler.java     # 全局异常处理
    │   ├── log
    │   │   ├── TraceIdFilter.java              # traceId 过滤器
    │   │   └── TraceIdUtils.java               # MDC 工具类
    │   └── model
    │       └── User.java                       # 实体类（与 01 模块相同）
    └── resources
        ├── application.yaml                    # server.port = 8081
        └── logback-spring.xml                  # 日志格式、appender、多环境
```

> 上面是**迁移前**的结构。迁移后这些类都搬到了 [00-springboot-common](../00-springboot-common/README.md)，本模块只剩启动类、`controller`、`model` 和 `application.yaml`，POM 里多一行对公共库的依赖。

模块 POM 只声明了 `parent`，**没有再引额外依赖**：`TraceIdFilter` 里用到的 `commons-lang3` 是父 POM 声明的公共依赖（版本由父 POM 的 BOM 管理）。（这是迁移前的情况；迁移后本模块的 POM 里多了一行对 `00-springboot-common` 的依赖。）

## 1. 统一响应体 `Result<T>`

```java
@Data
public class Result<T> {
    private static final String SUCCESS_CODE = "SUC0000";

    private String code;     // 状态码，SUC0000 表示成功
    private String message;  // 提示信息
    private T data;          // 业务数据
}
```

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码（字符串，**不是** HTTP 状态码），成功固定为 `SUC0000` |
| `message` | 提示信息，成功时是「操作成功」 |
| `data` | 业务数据，泛型 `T`；失败时为 `null`（Jackson 默认会把 null 输出成 `"data":null`） |

工厂方法（统一由私有 `build` 组装）：

| 方法 | 说明 |
| --- | --- |
| `Result.ok()` | 成功，`data` 为 `null` |
| `Result.ok(T data)` | 成功并携带数据 |
| `Result.fail()` | 失败，等价于 `fail(SYSTEM_ERROR.getCode(), SYSTEM_ERROR.getMessage())`，即 `SYS0001` + 「系统内部异常」 |
| `Result.fail(String code, String message)` | 失败，`code` / `message` 由调用方显式给出 |

**为什么 `fail` 要显式传 code 和 message，而不是直接把异常丢进来**：这样一个接口的失败文案完全由「错误码 + 业务文案」决定，异常对象里的类名、堆栈、SQL 报错等信息不会被顺手透出给客户端。

## 2. 错误码枚举 `ErrorCode`

```java
@Getter
public enum ErrorCode {

    QUERY_ERROR("QRY0001", "查询异常: {0}"),

    SYSTEM_ERROR("SYS0001", "系统内部异常"),

    REQUEST_ERROR("SYS0002", "请求异常");
}
```

| 错误码 | 含义 | 谁在用 |
| --- | --- | --- |
| `QRY0001` | 查询异常，message 带 `{0}` 占位符 | 业务异常 `BizException` |
| `SYS0001` | 系统内部异常 | `Result.fail()`，以及兜底 handler 对 5xx 的处理 |
| `SYS0002` | 请求异常 | `handleExceptionInternal` 对 4xx（405/400 等）的处理 |

- 枚举构造器**隐式就是 `private`**，所以能在常量后面直接写参数；
- `@Getter` 让外部通过 `getCode()` / `getMessage()` 取值；
- `{0}` 是 `MessageFormat` 的占位符，由 `BizException` 填充（见第 3 节）。

## 3. 业务异常 `BizException`

```java
@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorMessage;

    public BizException(ErrorCode errorCode) { ... }                 // 用枚举自带的 message
    public BizException(ErrorCode errorCode, Object... message) {    // 用参数填充 {0}、{1}…
        this.errorMessage = MessageFormat.format(errorCode.getMessage(), message);
    }
}
```

| 要点 | 说明 |
| --- | --- |
| 继承 `RuntimeException` | 属于**非受检异常**，业务代码里不用 `try-catch`，方法签名也不用 `throws` |
| 两个字段 | `errorCode` 记录错误类型，`errorMessage` 记录**格式化之后**的提示，`@Getter` 让全局异常处理器读到 |
| `Object...` 可变参数 | 支持任意个占位符参数。实测 `new BizException(ErrorCode.QUERY_ERROR, "查询用户为空！")` 最终返回 `"message":"查询异常: 查询用户为空！"` |
| `MessageFormat` 的坑 | 它用 `{0}` 占位，并且对**单引号**有特殊含义、对**数字**会做本地化格式化（`1234` → `1,234`），自己扩错误码时要留意 |

## 4. 响应包装 `ResponseWrapperAdvice`

```java
@ControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper;   // 构造器注入 Spring Boot 自动配置的 ObjectMapper
    ...
}
```

`ResponseBodyAdvice` 是 Spring MVC 的扩展点，**时机**是 `@ResponseBody` 方法的返回值被 `HttpMessageConverter` 序列化、写进响应体之前，可以在这里改写返回值——这就是实现统一响应包装的标准方案。

### 4.1 `supports()`：决定哪些响应要包装

返回 `true` 才会走到 `beforeBodyWrite()`。代码里的判断**有顺序**，前面的先短路：

| 顺序 | 判断 | 结果 |
| --- | --- | --- |
| 1 | 方法上有 `@IgnoreWrapper` | 不包装 |
| 2 | 所在类上有 `@IgnoreWrapper` | 不包装 |
| 3 | 返回类型是 `Result` | 不包装（避免重复包装） |
| 4 | 返回类型是 `ResponseEntity` | 不包装（文件下载这类响应不能被 JSON 包起来） |
| 5 | 返回类型是 `String` | **包装**（走 4.3 的特殊逻辑） |
| 6 | 其他类型：转换器是 `MappingJackson2HttpMessageConverter` | 包装；否则不包装 |

顺序很关键：第 5 条必须放在第 6 条前面，因为 `String` 用的转换器是 `StringHttpMessageConverter`，按第 6 条会被排除掉；而 1～4 是「排除项」，必须放最前面。

### 4.2 `beforeBodyWrite()`：真正改写返回值

```java
if (body instanceof Result) return body;                     // 已经是 Result，原样返回
if (returnType.getParameterType() == String.class) {         // String 特殊处理
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return objectMapper.writeValueAsString(Result.ok(body));
}
return Result.ok(body);                                      // 其他类型统一包装
```

`supports()` 已经排除了 `Result`，这里再判一次 `body instanceof Result` 是双重保险。

### 4.3 为什么 `String` 要特殊处理

- Controller 返回 `String` 时，Spring MVC 选中的是 `StringHttpMessageConverter`，它会把返回值**直接当字符串**写出去（内部相当于 `(String) body` 强转）；
- 所以这里不能直接 `return Result.ok(body)`，否则抛 `ClassCastException: Result cannot be cast to String`；
- 改成用 `ObjectMapper` **手动序列化成 JSON 字符串**再返回，同时把 `Content-Type` 改成 `application/json`（不改的话 `StringHttpMessageConverter` 会按 `text/plain` 输出）。

实测 `GET /api/hello`：`Content-Type: application/json`，响应体 `{"code":"SUC0000","message":"操作成功","data":"Hello World!"}`。

> 注入的 `ObjectMapper` 就是 Spring Boot 自动配置的那个，所以全局 Jackson 配置（如 01 模块用到的 `@JsonFormat`）在这里同样生效。

### 4.4 跳过包装 `@IgnoreWrapper`

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreWrapper {
}
```

| 元注解 | 作用 |
| --- | --- |
| `@Target({METHOD, TYPE})` | 既能标在方法上，也能标在整个 Controller 上 |
| `@Retention(RUNTIME)` | **必须**运行时保留：`supports()` 是用反射（`hasMethodAnnotation` / `isAnnotationPresent`）读它的，默认的 `CLASS` 级别读不到 |

## 5. 全局异常处理 `GlobalExceptionHandler`

### 5.1 三个 handler 的分工

| 方法 | 处理什么 | HTTP 状态码 | 返回的 code |
| --- | --- | --- | --- |
| `handleException(BizException)` | 业务异常 | `200` | `QRY0001` 等业务码（message 用格式化后的文案） |
| `handleExceptionInternal(...)` | **框架异常**（405/400/415…） | 保留框架原本的状态码 | 4xx → `SYS0002`；5xx → `SYS0001` |
| `handleException(Exception)` | 其他未知异常（NPE、SQL 异常…） | `200` | `SYS0001` |

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleException(BizException be) {
        log.warn("[{}] {}", be.getErrorCode(), be.getErrorMessage(), be);
        return Result.fail(be.getErrorCode().getCode(), be.getErrorMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.fail();
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        // 4xx 记 WARN（不打堆栈），5xx 记 ERROR（带堆栈）
        ...
        ErrorCode errorCode = status.is4xxClientError() ? ErrorCode.REQUEST_ERROR : ErrorCode.SYSTEM_ERROR;
        return new ResponseEntity<>(Result.fail(errorCode.getCode(), errorCode.getMessage()), headers, status);
    }
}
```

| 要点 | 说明 |
| --- | --- |
| `@RestControllerAdvice` | 等价于 `@ControllerAdvice` + `@ResponseBody`，handler 的返回值直接写进响应体 |
| 匹配顺序 | 找**最具体**的匹配：`BizException` → 业务 handler；框架异常 → 基类内置的 `@ExceptionHandler`；其余 → `Exception` 兜底 |
| 继承 `ResponseEntityExceptionHandler` | 基类已经内置了 `HttpRequestMethodNotSupportedException`、`HttpMessageNotReadableException`、`MissingServletRequestParameterException`、`MethodArgumentNotValidException`、`NoHandlerFoundException` 等一整套处理，并且**带正确的 HTTP 状态码** |
| 为什么要重写 `handleExceptionInternal` | 基类所有内置处理最后都汇聚到这个方法，重写它就能在**保留状态码和响应头**的前提下把响应体换成统一的 `Result`。返回值类型必须是 `ResponseEntity<Object>`（基类签名如此，泛型不协变，所以用 `new ResponseEntity<>(...)`） |
| 日志分流 | 4xx 记 `WARN`（只有一行 message，不打堆栈），5xx 记 `ERROR`（带堆栈），便于错误日志文件只收真正的故障 |
| 脱敏 | 客户端只拿到「错误码 + 固定文案」，异常类名、堆栈、SQL 报错只进日志 |

### 5.2 统一响应约定：业务失败也是 HTTP `200`

**业务成功还是失败，一律看响应体里的 `code`；业务异常也返回 `200`。** 实测：

```
POST /error/user  ->  HTTP/1.1 200
{"code":"QRY0001","message":"查询异常: 查询用户为空！","data":null}
```

实现基础是 `@ExceptionHandler` 的语义：handler 方法正常返回时，Spring 不会去改 HTTP 状态码。好处是前端只需要一套判定标准（看 `code`），不用同时处理「HTTP 状态码」和「业务码」两套体系。

注意这个约定**只覆盖业务异常和未知异常**：框架层面的异常（方法不支持、参数缺失、请求体解析失败等）走 `handleExceptionInternal`，会保留真实的 4xx 状态码，响应体仍然是统一的 `Result`。实测：

| 请求 | 实测 |
| --- | --- |
| `POST /api/hello`（该方法只有 `@GetMapping`） | `405` + `Allow: GET` + `{"code":"SYS0002","message":"请求异常","data":null}` |
| `GET /download`（缺少必填参数 `filename`） | `400` + `{"code":"SYS0002","message":"请求异常","data":null}` |
| `POST /api/user1`（请求体不是合法 JSON） | `400` + `{"code":"SYS0002","message":"请求异常","data":null}` |
| `POST /error/user`（业务异常） | `200` + `{"code":"QRY0001","message":"查询异常: 查询用户为空！","data":null}` |

## 6. 统一日志（基础版）

### 6.1 `TraceIdFilter`：让一次请求的日志能串起来

```java
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) ... {
        String traceId = req.getHeader(TraceIdUtils.TRACE_ID);   // 优先取上游传入
        if (StringUtils.isBlank(traceId)) {
            traceId = TraceIdUtils.generate();                   // 没有就自己生成
        }
        TraceIdUtils.setTraceId(traceId);
        resp.setHeader(TraceIdUtils.TRACE_ID, traceId);          // 回写响应头
        long start = System.currentTimeMillis();
        try {
            log.info("请求开始 {} {}", req.getMethod(), req.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("请求结束 {} {} 耗时={}ms", req.getMethod(), req.getRequestURI(), cost);
            TraceIdUtils.clear();                                // 必须清理
        }
    }
}
```

| 要点 | 说明 |
| --- | --- |
| `Filter` 是最外层 | 请求一进来就设置 traceId，后面 Controller、Advice 里打的日志都能带上它 |
| `@Order(Ordered.HIGHEST_PRECEDENCE)` | 让它在过滤器链中优先级最高；`@Component` 让 Spring Boot 自动注册这个 Filter |
| 优先取上游 header | 微服务调用链里上游会传 `traceId`（通常由网关生成），下游复用它才能串成一条链路；没有才自己生成 |
| 回写响应头 | 前端报问题时把响应头里的 `traceId` 一起给出，后端可以直接按它检索日志 |
| `finally` 里 `clear()` | Servlet 线程是**池化复用**的，不清理会导致下一个请求的日志带上上一个请求的 traceId（串号） |
| 耗时统计 | 用 `System.currentTimeMillis()` 前后差值记录；更精细的场景可以用 Micrometer |

实测（带上游 traceId）：

```bash
curl -i -H 'traceId: my-trace-0001' http://localhost:8081/api/hello
# 响应头：traceId: my-trace-0001
# 日志：  ... [my-trace-0001] com.hyao.advice.log.TraceIdFilter - 请求开始 GET /api/hello
#        ... [my-trace-0001] com.hyao.advice.log.TraceIdFilter - 请求结束 GET /api/hello 耗时=2ms
```

### 6.2 `TraceIdUtils`：MDC 的薄封装

| 方法 / 常量 | 作用 |
| --- | --- |
| `TRACE_ID = "traceId"` | MDC 和 HTTP header 共用的 key |
| `setTraceId(String)` | `MDC.put(TRACE_ID, traceId)`，把值绑到当前线程 |
| `getTraceId()` | `MDC.get(TRACE_ID)` |
| `clear()` | `MDC.clear()` |
| `generate()` | `UUID.randomUUID()` 去掉横线，32 位十六进制字符串 |

类声明为 `final` 且构造器私有，是工具类的常规写法。**MDC 底层是 ThreadLocal**，所以 traceId 只在当前请求线程内可见——异步线程、线程池里需要手动传递，这也是后续「生产级统一日志」要解决的问题之一。

### 6.3 `logback-spring.xml`：日志格式与落盘

日志格式（`LOG_PATTERN`）：

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] %logger{40} - %msg%n
```

| 片段 | 含义 |
| --- | --- |
| `%d{...}` | 时间戳（毫秒） |
| `[%thread]` | 线程名，如 `http-nio-8081-exec-2` |
| `%-5level` | 日志级别，左对齐补 5 位 |
| `[%X{traceId:-}]` | **MDC 里的 traceId**，取不到时显示 `-` |
| `%logger{40}` | 日志类名，超过 40 字符会缩写，如 `c.h.a.exception.GlobalExceptionHandler` |
| `%msg%n` | 消息 + 换行 |

三个 appender：

| appender | 输出 | 滚动策略 |
| --- | --- | --- |
| `CONSOLE` | 控制台 | — |
| `FILE` | `./logs/${APP_NAME}.log` | `SizeAndTimeBasedRollingPolicy`：单文件 100MB、保留 30 天、总量上限 5GB，历史文件 gzip 到 `./logs/history/` |
| `ERROR_FILE` | `./logs/${APP_NAME}-error.log` | 同上，保留 60 天；配了 `LevelFilter`（`ERROR` → ACCEPT，其他 → DENY），**只有 ERROR 级别会进来** |

```xml
<springProperty scope="context" name="APP_NAME"
                source="spring.application.name" defaultValue="app"/>
```

- `APP_NAME` 取自 `spring.application.name`；本项目 `application.yaml` 里没配，所以用默认值 `app`，实测生成的是 `./logs/app.log` 和 `./logs/app-error.log`；
- `./logs/` 是**相对应用工作目录**的路径（在 IDEA 里运行就是模块目录下）；
- 多环境用 `springProfile` 区分：`dev` → 控制台 + 文件，`prod` → 文件 + 错误文件，`!dev & !prod`（默认，未激活任何 profile 时）→ 控制台 + 文件。

第三方包降噪：

```xml
<logger name="org.springframework" level="WARN"/>
<logger name="org.mybatis" level="WARN"/>
<logger name="com.zaxxer.hikari" level="WARN"/>
```

把框架日志压到 `WARN` 以上，避免启动和运行时的 INFO 淹没业务日志。实测的副作用：`Tomcat started on port(s): 8081`、`Tomcat initialized with port(s)` 这类由 `org.springframework.*` 打的 INFO 不再输出（实测 0 行），而 `Started AdviceApplication in x seconds` 这种用**应用自身 logger** 打的启动日志仍然会输出。

> `org.mybatis`、`com.zaxxer.hikari` 这两个 logger 目前还没有对应的依赖（03/04 模块才会用到），属于提前配置。

### 6.4 实测的一条完整日志

```
2026-10-12 23:12:27.977 [http-nio-8081-exec-8] WARN  [2c783666492a4c8aa2222f5a1ad4b799] c.h.a.exception.GlobalExceptionHandler - [400] uri=/api/user1 JSON parse error: Unexpected character ('o' (code 111)): ...
```

一次请求的「开始 / 业务处理 / 结束」共享同一个 `[traceId]`，出问题时按 traceId 检索就能把整条链路捞出来。

## 7. Controller 示例与接口一览

### 7.1 `UserController`：四种返回写法

| 方法 | 返回值 | 结果 |
| --- | --- | --- |
| `GET /api/hello` | `String` | 被包装成 `Result`，`Content-Type` 变成 `application/json` |
| `POST /api/user1` | `User` | 被自动包装成 `Result<User>` |
| `POST /api/user2` | `User` + `@IgnoreWrapper` | 不包装，直接返回 User 的 JSON |
| `POST /api/user3` | `Result<User>` | 已经包好，不再重复包装 |

`user1`、`user2`、`user3` 的请求体都是：

```json
{ "id": "0001", "name": "hyao", "age": 18, "birthday": "2008-10-11" }
```

### 7.2 `ErrorController`：抛业务异常

```java
@PostMapping("/user")
public User getUser() {
    throw new BizException(ErrorCode.QUERY_ERROR, "查询用户为空！");
}
```

方法体里没有 try-catch，异常交给 `GlobalExceptionHandler`。注意返回类型写的是 `User`，实际响应体是 `Result`——异常路径上返回的是 handler 的返回值。

### 7.3 `DownloadController`：文件下载跳过包装

```java
@GetMapping("/download")
public ResponseEntity<Resource> download(@RequestParam String filename) {
    Resource resource = new ClassPathResource(filename);
    if (!resource.exists()) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(resource.contentLength())
            .body(resource);
}
```

| 要点 | 说明 |
| --- | --- |
| 为什么用 `ResponseEntity<Resource>` | 文件是二进制流，不能被 `Result` 包成 JSON；`supports()` 第 4 条专门排除了 `ResponseEntity` |
| `ClassPathResource` | 从 classpath 读资源，所以 `application.yaml`（在 `resources` 根目录、编译后在 classes 根目录）可以直接下载 |
| `ContentDisposition.attachment()` | 告诉浏览器「下载」而不是「在页面里打开」；指定 `UTF_8` 是为了中文文件名，实测响应头是 `Content-Disposition: attachment; filename*=UTF-8''application.yaml` |
| 文件不存在 | `ResponseEntity.notFound()` → 实测 `404`。这是 Controller **主动返回**的响应，不是异常，所以不走全局异常处理 |
| `IOException` | 捕获后 `throw new BizException(ErrorCode.SYSTEM_ERROR)`，同样交给全局异常处理 |

### 7.4 接口一览（实测，端口 8081）

| 请求 | 说明 | 实测结果 |
| --- | --- | --- |
| `GET /api/hello` | `String` 被包装 | `200`，`application/json`，响应头带 `traceId` |
| `POST /api/user1` | `User` 被自动包装 | `200`，`{"code":"SUC0000","message":"操作成功","data":{…}}` |
| `POST /api/user2` | `@IgnoreWrapper` 跳过包装 | `200`，裸 User JSON（没有 `code`/`message` 外层） |
| `POST /api/user3` | 已是 `Result`，不重复包装 | `200`，与 `user1` 结构一致 |
| `POST /error/user` | 抛 `BizException` | `200`，`{"code":"QRY0001","message":"查询异常: 查询用户为空！","data":null}` |
| `POST /api/hello` | 方法不支持（框架异常） | `405` + `Allow: GET`，`{"code":"SYS0002","message":"请求异常","data":null}` |
| `GET /download` | 缺必填参数（框架异常） | `400`，`{"code":"SYS0002",…}` |
| `POST /api/user1` | 请求体非法 JSON（框架异常） | `400`，`{"code":"SYS0002",…}` |
| `GET /download?filename=application.yaml` | 文件下载 | `200`，`application/octet-stream` |
| `GET /download?filename=x.txt` | 文件不存在 | `404`（Controller 主动返回） |

启动方式：在 IDEA 里运行 [`AdviceApplication`](src/main/java/com/hyao/advice/AdviceApplication.java) 的 `main` 方法，端口 8081。

## 8. 与 01 模块的差异

| 项 | 01-springboot-base | 02-springboot-advice |
| --- | --- | --- |
| 端口 | 8080 | 8081（两个模块可以同时启动） |
| 返回值 | 原样输出（`Map` / POJO / `String`） | 统一包成 `Result<T>` |
| 异常 | 不处理，走框架默认响应 | 全局处理：业务异常 `200` + 业务码；框架异常保留 4xx + `SYS0002` |
| 日志 | 默认 Logback 配置 | traceId 贯穿 + 统一格式 + 按天/大小滚动 + 错误单独文件 |
| 额外依赖 | 无 | 无（`commons-lang3` 来自父 POM） |

## 9. 小结

| 知识点 | 位置 |
| --- | --- |
| 统一响应体与静态工厂方法 | `Result` |
| 错误码枚举、`MessageFormat` 占位符 | `ErrorCode` |
| 非受检业务异常、可变参数构造器 | `BizException` |
| `ResponseBodyAdvice` 改写返回值、`supports` / `beforeBodyWrite` | `ResponseWrapperAdvice` |
| `String` 返回值为什么必须手动序列化 | `ResponseWrapperAdvice.beforeBodyWrite` |
| 自定义注解 + `@Target` / `@Retention(RUNTIME)` | `IgnoreWrapper` |
| `@RestControllerAdvice` + `@ExceptionHandler`、异常匹配「最具体优先」 | `GlobalExceptionHandler` |
| 继承 `ResponseEntityExceptionHandler` 保留框架状态码、重写 `handleExceptionInternal` | `GlobalExceptionHandler` |
| 业务异常也返回 `200`、成功失败看 `code` | 5.2 节 |
| `Filter` + `MDC` 实现 traceId 贯穿、`finally` 清理防串号 | `TraceIdFilter`、`TraceIdUtils` |
| Logback 格式、`%X{traceId}`、滚动策略、`springProfile` 多环境 | `logback-spring.xml` |
| 文件下载返回 `ResponseEntity`、主动返回 `404` | `DownloadController` |

