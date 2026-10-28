# 02-springboot-advice

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 1. 学习目标

本模块是**示例模块**：三项横切能力的实现都在公共库 `00-springboot-common`（包名 `com.hyao.common.*`），这里只保留 Controller 和配置，用来演示效果。学完应能回答：

- 一个 `ResponseBodyAdvice` 怎么把所有接口的返回值统一包成 `Result<T>`，哪些返回类型必须排除；
- 全局异常处理怎么分层：业务异常、框架异常（405/400）、未知异常分别返回什么；
- 「业务失败也是 HTTP 200、成功失败看 `code`」这个约定是怎么实现的；
- 业务错误码怎么扩展（`IErrorCode` + 业务侧枚举）；
- `traceId` 怎么贯穿一次请求；这些能力为什么要抽成公共库、怎么靠**自动配置**做到"加一行依赖即生效"。

## 2. 学习清单

| 状态 | 优先级 | 知识点 | 说明 |
| --- | --- | --- | --- |
| ✅ | — | **统一响应包装（示例）** | 00 的 `ResponseWrapperAdvice`：`GET /api/hello`、`POST /api/user1` 自动包装成 `Result<T>`；`/api/user2` 用 `@IgnoreWrapper` 跳过；`/api/user3` 已是 `Result` 不重复包装 |
| ✅ | — | **全局异常处理（示例）** | 00 的 `GlobalExceptionHandler`：业务异常 → `200` + 业务码（`BizException`）；框架异常保留真实状态码：405 → `SYS0002`、404 → `SYS0003`、400 → `SYS0004` / `SYS0005` |
| ✅ | — | **参数校验（示例）** | 00 的 Bean Validation 统一处理：`POST /api/user/valid` 标 `@Valid`，`name` 必填且不超过 10 个字符、`age` 在 1~150 之间，校验失败 → `400` + `SYS0004`（`参数校验失败: name: 不能为空`）；其它接口没标 `@Valid`，同样的 `UserEntity` 不会被校验 |
| ✅ | — | **统一日志（示例）** | 00 的 `TraceIdFilter` + 默认 `logback-spring.xml`：日志与响应头都带 `traceId` |
| ✅ | — | **自定义业务错误码** | `BizErrorCode implements IErrorCode`（`QRY0001` 查询、`FDL0001` 下载），业务错误码由业务模块自己维护 |
| ✅ | — | **包装的边界：下载与跳过开关** | `DownloadController` 返回 `ResponseEntity<Resource>`，被 `supports()` 排除；`@IgnoreWrapper`（00 模块）方法级/类级都能标 |

## 3. 依赖与配置

| 依赖 | 说明 | 版本 |
| --- | --- | --- |
| `com.hyao.common:00-springboot-common` | 三项能力的实现（响应包装 / 全局异常 / traceId 日志） | 父 POM 统一管（`${project.version}`） |
| `spring-boot-starter-web`、`spring-boot-devtools`、`lombok` 等 | 本模块不声明，全部继承父 POM 的公共依赖 | 父 POM / BOM |

POM 里只多了一行对公共库的依赖，注释也写明了：**不需要 `scanBasePackages`**，靠 00 的自动配置生效。`src/main/resources/application.yaml` 全文只有端口：

```yaml
server:
  port: 8082
```

- 端口以这个文件为准；`UserController`、`ErrorController`、`DownloadController` 的 javadoc 里还写着 `8081`，那是早期端口，属于注释残留；
- 三项能力**零配置**：`Result`、异常处理、traceId 都不需要在本模块写任何 `@Bean` 或配置项。

## 4. 核心实现

### 4.1 `AdviceApplication`

```java
@SpringBootApplication
public class AdviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdviceApplication.class, args);
    }
}
```

- 本模块**没有任何** `@ControllerAdvice` / `@Bean` 配置：三项能力全部由 00 模块的 `CommonWebAutoConfiguration` 通过 `AutoConfiguration.imports` 自动注册；
- 类的 javadoc 记了版本变更：1.0 三项能力在本模块自实现 → 2.0 抽到公共库（见第 7 节）。

### 4.2 `UserController`（`/api`）

| 接口 | 返回值 | 包装结果 |
| --- | --- | --- |
| `GET /api/hello` | `String` | 包成 `Result`，`Content-Type` 变成 `application/json` |
| `POST /api/user1` | `UserEntity` | 自动包成 `Result<User>` |
| `POST /api/user2` | `UserEntity` + `@IgnoreWrapper` | 不包装，直接返回 User 的 JSON |
| `POST /api/user3` | `Result<User>` | 已经包好，不重复包装 |
| `POST /api/user/valid` | `UserEntity` + `@Valid` | 校验通过则自动包装；失败由 00 统一返回 `400` + `SYS0004` |

这几个 POST 的请求体相同：`{ "id": "0001", "name": "hyao", "age": 18, "birthday": "2008-10-11" }`。

这些方法里**没有任何包装/校验代码**（除了 `@Valid` 注解）：包装靠 `ResponseBodyAdvice`，校验失败靠 00 的统一异常处理。

### 4.3 `ErrorController`（`/error`）

```java
@PostMapping("/user")
public User getUser() {
    throw new BizException(BizErrorCode.QUERY_ERROR, "查询用户为空！");
}
```

没有 try-catch，异常交给 00 的全局异常处理。注意返回类型写的是 `UserEntity`，实际响应体是 `Result`——异常路径上返回的是 handler 的返回值，不是这个方法的。

### 4.4 `DownloadController`（`/download`）

```java
@GetMapping("/download")
public ResponseEntity<Resource> download(@RequestParam String filename) {
    Resource resource = new ClassPathResource(filename);
    if (!resource.exists()) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(resource.contentLength())
            .body(resource);
}
```

- 二进制流不能被 `Result` 包成 JSON，所以返回 `ResponseEntity`——00 的 `supports()` 会直接排除 `ResponseEntity`；
- `ClassPathResource` 从 classpath 读，`application.yaml` 这种在 classpath 根目录的文件可以直接下载；
- 文件不存在时**主动**返回 404（`ResponseEntity.notFound()`）——这是响应不是异常，不走全局异常处理；`IOException` 时抛 `BizException(BizErrorCode.FILE_DOWNLOAD_ERROR)`（`FDL0001`），交给全局异常处理。

### 4.5 `BizErrorCode`

```java
@Getter
public enum BizErrorCode implements IErrorCode {
    QUERY_ERROR("QRY0001", "查询错误: {0}"), // 演示错误信息占位符
    FILE_DOWNLOAD_ERROR("FDL0001", "文件下载异常"),
    ;
    ...
}
```

- 实现 00 模块的 `IErrorCode`（`getCode()` / `getMessage()`）：**业务错误码由业务模块自己维护，公共库只放系统级错误码**（`SysErrorCode`：`SYS0001` 系统内部异常、`SYS0002` 请求方法不支持、`SYS0003` 接口不存在、`SYS0004` 参数校验失败、`SYS0005` 参数类型不匹配、`SYS0006` 请求异常兜底）；
- `QUERY_ERROR` 的文案带 `{0}` 占位符，`BizException` 会用 `MessageFormat` 把传入的参数填进去。

## 5. 快速验证

在 IDEA 里运行 `AdviceApplication` 的 `main` 方法，端口见 `application.yaml`（当前 `8082`）。

| 请求 | 说明 | 结果 |
| --- | --- | --- |
| `GET /api/hello` | `String` 被包装 | `200`，`application/json`，响应头带 `traceId`，body `{"code":"SUC0000","message":"操作成功","data":"Hello World!"}` |
| `POST /api/user1` | `UserEntity` 自动包装 | `200`，`{"code":"SUC0000","message":"操作成功","data":{...}}` |
| `POST /api/user2` | `@IgnoreWrapper` | `200`，裸 User JSON（没有 `code` / `message` 外层） |
| `POST /api/user3` | 已是 `Result`，不重复包装 | `200`，与 `user1` 结构一致（只有一层 `Result`） |
| `POST /error/user` | 业务异常 | `200`，`{"code":"QRY0001","message":"查询错误: 查询用户为空！","data":null}` |
| `POST /api/user/valid`，body 缺 `name` | 参数校验失败 | `400`，`{"code":"SYS0004","message":"参数校验失败: name: 不能为空","data":null}` |
| `POST /api/user/valid`，`age` 传 `200` | 参数校验失败 | `400`，`{"code":"SYS0004","message":"参数校验失败: age: 不能大于 150","data":null}` |
| `POST /api/user/valid`，body 不是合法 JSON | 请求体解析失败 | `400`，`{"code":"SYS0004","message":"参数校验失败，请求体解析失败","data":null}` |
| `POST /api/hello` | 方法不支持（框架异常） | `405` + `Allow: GET` + `{"code":"SYS0002","message":"请求方法不支持，该接口仅支持 GET","data":null}` |
| `GET /nope` | URL 不存在 | `404`，`{"code":"SYS0003","message":"接口不存在: /nope","data":null}` |
| `GET /download?filename=application.yaml` | 文件下载 | `200`，`application/octet-stream` |
| `GET /download?filename=x.txt` | 文件不存在 | `404`（Controller 主动返回） |

两个细节：每个响应都带 `traceId`（00 的 `TraceIdFilter` 回写），排查问题可直接按它检索日志；405 会带 `Allow: GET`——框架异常保留标准 HTTP 语义，只是响应体换成了 `Result`。

## 6. 常见问题与踩坑

- **`MessageFormat` 的占位符**：`QUERY_ERROR` 的文案是 `查询错误: {0}`，参数会被填进去（返回 `查询错误: 查询用户为空！`）；但**文案里没有占位符时，`new BizException(code, "参数")` 传的参数会被静默忽略**——扩错误码时留意。另外 `MessageFormat` 把单引号当转义、会给数字加千分位（`1234` → `1,234`）。
- **`String` 返回值为什么要特殊处理**：`StringHttpMessageConverter` 会把返回值当字符串强转，直接返回 `Result` 会 `ClassCastException`；00 的包装器在 String 分支用 `ObjectMapper` 手动序列化并改 `Content-Type`，所以 `GET /api/hello` 返回的是 JSON。
- **业务异常与框架异常的 HTTP 状态码不一样**：业务异常 `200` + 业务码；框架异常（405/404/400/415…）保留状态码 + 细分后的系统码（`SYS0002` 方法不支持、`SYS0003` 接口不存在、`SYS0004` 参数校验失败、`SYS0005` 类型不匹配、`SYS0006` 其余 4xx）。前端只看状态码会把两者混在一起，所以约定是「业务看 code、协议看 status」。
- **`DownloadController` 的 404 不走全局异常处理**：`ResponseEntity.notFound()` 是主动返回的响应，不是异常。
- **`@IgnoreWrapper` 靠运行时反射读**：注解必须是 `@Retention(RUNTIME)`（00 模块已如此定义），否则 `supports()` 读不到。

## 7. 学习笔记

### 迁移说明：三项能力为什么抽到了公共库

**事由**：这三项能力最早直接写在本模块里；后面的模块（03、04…）需要同一套约定，为了不再复制代码，整体抽到 [`00-springboot-common`](../00-springboot-common/README.md)。

**直接依赖本模块来复用会踩的坑**：

1. **自家模块的版本必须自己写**：`spring-boot-dependencies` 这个 BOM 只管第三方依赖，`com.hyao.advice:02-springboot-advice` 是自家模块，不写 `<version>` 会让**整个工程**的 Maven 模型都读不出来；而且要用 `${project.version}`，不是 `spring.boot.version`；
2. **位置容易写错**：写在父 POM 的 `<dependencies>` 里会被**所有**子模块继承——本模块会依赖自己，01 模块也会被塞进包装 / 异常逻辑；
3. **依赖方默认扫不到**：这些类在 `com.hyao.advice.*` 包下，别的模块的启动类扫描不到，`@ControllerAdvice` 也就不会注册；
4. **可执行 jar 用不了**：若本模块产出可执行 jar，类会被放进 `BOOT-INF/classes`，别的模块编译期根本找不到。

**最终做法**：搬到 00 模块并做成 **starter 式自动配置**（`AutoConfiguration.imports` + `@ConditionalOnMissingBean`）——依赖方加一行依赖即生效，**不需要 `scanBasePackages`**；本模块退化为示例模块。

| 迁移项 | 迁移前（本模块） | 迁移后（00 模块） |
| --- | --- | --- |
| 根包名 | `com.hyao.advice` | `com.hyao.common` |
| 统一响应体 / 错误码契约 | `advice.common.Result` | `common.model.Result`、`common.exception.IErrorCode` / `SysErrorCode` |
| 业务异常 / 全局异常处理 | `advice.common.BizException`、`advice.exception.GlobalExceptionHandler` | `common.exception.BizException`、`GlobalExceptionHandler` |
| 响应包装 / 跳过注解 | `advice.advice.*` | `common.advice.ResponseWrapperAdvice`、`IgnoreWrapper` |
| traceId 与日志配置 | `advice.log.*`、`logback-spring.xml` | `common.log.*`、`logback-spring.xml` |
| 生效方式 | 包扫描（`@Component`、同包 `@ControllerAdvice`） | `AutoConfiguration.imports` 自动配置 |

### 本模块的定位

- **只供学习**：包装、异常、日志都是"能跑通的最小实现"，真实项目要具体场景具体分析（响应要不要统一包、包哪些接口、异常要不要保留 HTTP 状态码、日志怎么落盘与采集）；
- **接口约定（自本模块起）**：业务成功还是失败一律看响应体里的 `code`（`SUC0000` 成功），业务异常连 HTTP 状态码也是 `200`；请求/参数层面的异常保留真实状态码（405/404/400）并用细分系统码（`SYS0002`~`SYS0006`），参数校验失败统一 `400 + SYS0004`。三项能力的实现原理都在 00 模块，本模块只演示"用了之后是什么效果"。
