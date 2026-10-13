# SpringBoot-Learning

> ⚠️ **本 README 由 AI 自动生成**（随代码变更同步更新），用于梳理本项目已有代码对应的知识点，内容按实际代码与实测结果核对过，但仍可能有疏漏或滞后，**如与代码不一致请以 [pom.xml](pom.xml) 和源码为准**。

Spring Boot 学习项目。采用 **Maven 多模块聚合工程** 组织，按知识点拆分模块，一个模块对应一个主题，便于逐步学习和对比。

## 模块规划

| 模块 | 主题 | 状态 |
| --- | --- | --- |
| [00-springboot-common](00-springboot-common/README.md) | 公共库：响应包装 + 全局异常 + 统一日志（自动配置） | ✅ 已完成 |
| [01-springboot-base](01-springboot-base/README.md) | 基础入门：启动类、配置文件、静态资源、Spring MVC / REST 控制器 | ✅ 已完成 |
| [02-springboot-advice](02-springboot-advice/README.md) | 公共能力的示例模块：用 Controller 演示包装 / 异常 / traceId | ✅ 已完成 |
| 03-springboot-mybatis-druid | 持久层 MyBatis + Druid 连接池 | ⬜ 进行中 |
| 04-springboot-mybatis-plus | 持久层 MyBatis-Plus | ⬜ 规划中 |

> 后续模块只作为 `pom.xml` 中的 `<module>` 追加，约定沿用「`序号-springboot-主题`」的命名方式。

> **接口响应约定**（自 02 模块起）：业务成功还是失败**一律看响应体里的 `code`**——`SUC0000` 表示成功，其他值对应 `ErrorCode`；业务异常（`BizException`）连 HTTP 状态码也是 `200`。框架层面的异常（方法不支持 `405`、缺参数 / 请求体解析失败 `400` 等）会保留真实的 HTTP 状态码，但响应体同样是统一的 `Result`。后续模块都遵循这个约定，详见 [02 模块 README](02-springboot-advice/README.md)。

## 项目结构

```
SpringBoot-Learning
├── pom.xml                          # 父 POM：聚合模块 + 统一版本 + 公共依赖
├── 00-springboot-common             # 公共库：响应包装 / 全局异常 / 统一日志（自动配置）
├── 01-springboot-base               # 01 基础
├── 02-springboot-advice             # 02 公共能力的示例（依赖 00）
├── 03-springboot-mybatis-druid      # 03 MyBatis + Druid（依赖 00）
└── 04-springboot-mybatis-plus       # 04 预留（依赖 00）
```

## 技术栈与环境

| 项 | 版本 / 说明 |
| --- | --- |
| JDK | 8（父 POM 中 `maven.compiler.source/target=8`） |
| Maven | 3.6+ |
| Spring Boot | 2.7.18（父 POM 属性 `spring.boot.version` 统一指定） |
| IDE | IntelliJ IDEA，需安装 **Lombok 插件**（见下） |

## 父 POM 做了什么

父工程 `com.hyao:SpringBoot-Learning:1.0.0-SNAPSHOT` 的 `packaging` 为 `pom`，本身不打可执行包，只承担三件事：

1. **模块聚合**
   ```xml
   <modules>
       <module>00-springboot-common</module>
       <module>01-springboot-base</module>
       <module>02-springboot-advice</module>
   </modules>
   ```
   新增模块时在这里追加 `<module>` 即可，父工程会自动把它纳入同一个工程统一管理（依赖版本、公共依赖都跟着走）。

2. **统一版本（BOM）**
   ```xml
   <dependencyManagement>
       <dependencies>
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-dependencies</artifactId>
               <version>${spring.boot.version}</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>
       </dependencies>
   </dependencyManagement>
   ```
   通过 `scope=import` 导入 Spring Boot 官方 BOM，把 Spring Boot 全家桶（web、thymeleaf、mybatis、test 等）的版本一次性约定好。
   **好处**：子模块引入 Spring Boot 相关依赖时不需要再写 `<version>`，升级 Spring Boot 只改父 POM 一处，避免各模块版本冲突。

3. **公共依赖**
   `<dependencies>` 中声明的依赖会被 **所有子模块继承**，目前包括：

   | 依赖 | scope | 作用 |
   | --- | --- | --- |
   | `spring-boot-starter-web` | compile | Web 起步依赖（内嵌 Tomcat + Spring MVC + Jackson） |
   | `spring-boot-devtools` | runtime / optional | 热部署，代码变更后自动重启应用上下文 |
   | `spring-boot-configuration-processor` | compile / optional | 编译期生成配置元数据，让 IDE 在 `application.yaml` 里对自定义配置项做提示与补全 |
   | `lombok` | provided / optional | 编译期生成 getter/setter 等样板代码，不参与打包 |
   | `commons-io` | compile | 通用 IO 工具类。**不在 Spring Boot BOM 的管理范围内**，所以必须显式写 `<version>` |
   | `commons-lang3` | compile | 字符串 / 集合等工具类（02 模块的 `TraceIdFilter` 用了 `StringUtils`）。**在 BOM 管理范围内**，所以不写 `<version>` |

   带 `optional=true` 的三个依赖（`devtools`、`configuration-processor`、`lombok`）不会被传递给「依赖本项目的其他项目」；
   `devtools` 用 `runtime` 是因为只在运行时起作用，编译时用不到；
   `lombok` 用 `provided` 是因为它只参与编译，编译完成后 jar 包不需要打进去。

   另外父 POM 还统一了 `project.build.sourceEncoding=UTF-8`，避免中文注释/中文响应乱码。

## 环境准备

- **JDK 8+**，并配置 `JAVA_HOME`。
- **Maven 3.6+**（或直接使用 IDEA 内置的 Maven）。
- **IDEA 安装 Lombok 插件**：`Settings → Plugins → 搜索 Lombok` 安装并开启 `Annotation Processing`（`Settings → Build → Compiler → Annotation Processors → Enable annotation processing`）。
  否则 IDE 会报「找不到 `getXxx()` 方法」之类的方法不存在错误；这只影响 IDE 提示，不影响 Maven 编译。

## 快速开始

本项目以**代码层面的学习**为主，不涉及构建、打包与部署，所以这里不展开 Maven 命令。

git 拉下项目后，用 IDEA 打开（Maven 工程会自动导入），然后直接运行对应模块的启动类即可，比如 [`BaseApplication`](01-springboot-base/src/main/java/com/hyao/base/BaseApplication.java)。

各模块的端口写在各自的 `application.yaml` 里——[01 是 8080](01-springboot-base/src/main/resources/application.yaml)、[02 是 8081](02-springboot-advice/src/main/resources/application.yaml)，互不冲突，可以同时启动。

