# 00-springboot-common

> ⚠️ **本 README 由 AI 自动生成**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

公共库模块（**是库，不是应用**）：把「统一响应包装 / 全局异常 / 统一日志」三项能力做成 **starter 式自动配置**，其他模块加一行依赖就能用。

知识点讲解在 [02 模块 README](../02-springboot-advice/README.md)——那里用 Controller 演示这三项能力的效果。

## 包结构

| 包 | 内容 | 说明 |
| --- | --- | --- |
| `model` | `Result`、`ErrorCode` | 统一响应体 + 错误码枚举 |
| `com.hyao.common.exception` | `BizException`、`GlobalExceptionHandler` | 业务异常 + 全局异常处理（继承 `ResponseEntityExceptionHandler`） |
| `com.hyao.common.advice` | `IgnoreWrapper`、`ResponseWrapperAdvice` | 跳过包装注解 + 响应包装（`ResponseBodyAdvice`） |
| `com.hyao.common.log` | `TraceIdUtils`、`TraceIdFilter` | MDC 工具类 + traceId 过滤器 |
| `com.hyao.common.autoconfigure` | `CommonWebAutoConfiguration` | 把上面的 Bean 注册进使用方的容器 |

```
00-springboot-common
├── pom.xml
└── src/main
    ├── java/com/hyao/common
    │   ├── advice
    │   │   ├── IgnoreWrapper.java              # 自定义注解：跳过包装
    │   │   └── ResponseWrapperAdvice.java      # 响应包装（ResponseBodyAdvice）
    │   ├── autoconfigure
    │   │   └── CommonWebAutoConfiguration.java # 自动配置：注册下面这些 Bean
    │   ├── exception
    │   │   ├── BizException.java               # 业务异常
    │   │   └── GlobalExceptionHandler.java     # 全局异常处理
    │   ├── log
    │   │   ├── TraceIdFilter.java              # traceId 过滤器
    │   │   └── TraceIdUtils.java               # MDC 工具类
    │   └── result
    │       ├── ErrorCode.java                  # 错误码枚举
    │       └── Result.java                     # 统一响应体
    └── resources
        ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── logback-spring.xml                  # 默认日志配置
```

## 自动配置怎么工作

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 里只有一行：

```
com.hyao.common.autoconfigure.CommonWebAutoConfiguration
```

Spring Boot 启动时会扫描 classpath 上**所有** jar 里的这个文件（Spring Boot 2.7 起的新写法），把列出的类当成自动配置类加载。所以使用方只要依赖本 jar，下面的 Bean 就自动进容器，**不需要写 `@ComponentScan` / `scanBasePackages`**。

```java
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ResponseBodyAdvice.class, MappingJackson2HttpMessageConverter.class, ObjectMapper.class})
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResponseWrapperAdvice responseWrapperAdvice(ObjectMapper objectMapper) { ... }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() { ... }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilter traceIdFilter() { ... }
}
```

| 注解 | 作用 |
| --- | --- |
| `@AutoConfiguration` | 标记自动配置类（相当于 `@Configuration(proxyBeanMethods = false)`），必须靠 `AutoConfiguration.imports` 才会被加载 |
| `after = JacksonAutoConfiguration.class` | 排在 Jackson 自动配置之后，这样方法参数才能注入到 Spring Boot 自动配置的那个 `ObjectMapper` |
| `@ConditionalOnWebApplication(SERVLET)` | 只在 Servlet Web 应用里生效 |
| `@ConditionalOnClass({...})` | classpath 上缺 Spring MVC / Jackson 时整体跳过 |
| `@ConditionalOnMissingBean` | 使用方自己定义了同类型 Bean 时以使用方为准 |

两个细节：

- `@ControllerAdvice` 虽然是 `@Component` 的派生注解，但这里没走包扫描——Spring MVC 查找 advice 是按**容器里 Bean 的类型**看有没有 `@ControllerAdvice`，所以用 `@Bean` 注册一样生效；
- `TraceIdFilter` 因此也去掉了 `@Component`，只由这里注册，避免出现两个实例（Filter 的优先级仍由类上的 `@Order(HIGHEST_PRECEDENCE)` 决定）。

## 使用方式

在需要的模块里加依赖（版本由父 POM 的 `dependencyManagement` 统一管，所以不写 `<version>`）：

```xml
<dependency>
    <groupId>com.hyao.common</groupId>
    <artifactId>00-springboot-common</artifactId>
</dependency>
```

加完什么都不用配：

| 能力 | 效果 |
| --- | --- |
| 响应包装 | 接口返回值自动包成 `Result<T>`；加 `@IgnoreWrapper` 或返回 `ResponseEntity` / `Result` 时自动跳过 |
| 全局异常 | `throw new BizException(ErrorCode.QUERY_ERROR, "查询用户为空！")` 就得到统一错误响应；框架异常（405/400）保留真实状态码 |
| 统一日志 | 每个请求的日志都带 `traceId`，响应头也会回写 `traceId` |

## 日志配置

`logback-spring.xml` 也放在本模块，作为**默认日志配置**：

- 格式：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] %logger{40} - %msg%n`
- 控制台 + 文件（`./logs/`，按天/大小滚动）+ 错误单独文件；
- 使用方**自己写了 `logback-spring.xml` 时以使用方的为准**（应用自己的 `classes` 目录在 classpath 上排在 jar 前面）。

## 注意

- 本模块是「库」：没有启动类，也没有任何打包插件配置，产物就是**普通 jar**。别把它打成可执行 jar——那样类会进 `BOOT-INF/classes`，其他模块就依赖不了了。
- 依赖它的模块（02/03/04）自动获得上述行为；01 模块**故意不依赖**它，用来对比原生的 Spring MVC 行为。
