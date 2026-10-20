# SpringBoot-Learning

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**（随代码变更同步更新），用于梳理本项目已有代码对应的知识点，内容按实际代码与实测结果核对过，但仍可能有疏漏或滞后，**如与代码不一致请以 [pom.xml](pom.xml) 和源码为准**。

## 1. 项目总览

Spring Boot 学习项目，用 **Maven 多模块聚合工程** 按知识点拆分：**一个模块 = 一个主题**，模块内是「能跑起来的最小示例 + 说明」，模块之间可以横向对比（例如「01 原生返回」对比「02 统一响应包装」）。

几个特点：

- **代码优先**：每个模块都有启动类，改配置、打断点、看日志/监控就能验证结论，README 里的行为都是实跑过的；
- **不涉及构建、打包与部署**：父 POM 里没有引入 `spring-boot-maven-plugin`，直接在 IDEA 里运行启动类即可；
- [00-springboot-common](00-springboot-common/README.md) 是**公共库**（统一响应包装 / 全局异常 / 统一日志），02/03/04 依赖它；**01 故意不依赖**，用来演示 Spring MVC 的原生行为。

## 2. 模块导航

| 模块 | 主题 | 端口 | 状态 | README |
| --- | --- | --- | --- | --- |
| `00-springboot-common` | 公共库：响应包装 + 全局异常 + 参数校验 + 统一日志（starter 式自动配置） | 无（库） | ✅ 已完成 | [00 模块](00-springboot-common/README.md) |
| `01-springboot-base` | 基础入门：启动类、配置文件、静态资源、MVC / REST 控制器 | 8081 | ✅ 已完成 | [01 模块](01-springboot-base/README.md) |
| `02-springboot-advice` | 公共能力的示例：用 Controller 演示包装 / 异常 / traceId | 8082 | ✅ 已完成 | [02 模块](02-springboot-advice/README.md) |
| `03-springboot-mybatis` | 持久层：Spring Boot 集成 MyBatis + Druid（业务场景驱动的 MyBatis 特性） | 8083 | 🚧 进行中 | [03 模块](03-springboot-mybatis/README.md) |
| `04-springboot-mybatis-plus` | 持久层：MyBatis-Plus | — | ⬜ 规划中（暂未纳入 `<modules>`） | — |

> **命名约定**：`序号-springboot-主题`。新增模块 = 建目录 + 在父 POM 的 `<modules>` 里追加一行 + 写该模块的 README。

## 3. 统一环境

| 项 | 版本 / 说明 |
| --- | --- |
| JDK | 8（父 POM `maven.compiler.source/target=8`） |
| Maven | 3.6+（或直接用 IDEA 内置的 Maven） |
| Spring Boot | 2.7.18（父 POM 属性 `spring.boot.version` 统一指定） |
| IDE | IntelliJ IDEA，需安装 **Lombok 插件**并开启注解处理（`Settings → Build → Compiler → Annotation Processors → Enable annotation processing`），否则会报「找不到 `getXxx()` 方法」 |
| 数据库（03/04） | 本地 MySQL，库名 `hyao`，表结构与初始数据见各模块 `resources/script/1.sql` |
| 编码 | 父 POM 统一 `project.build.sourceEncoding=UTF-8`，避免中文注释/响应乱码 |

**端口约定**：各模块在各自的 `application.yaml` 里配置，互不冲突、可以同时启动——01 = 8081、02 = 8082、03 = 8083。注意同一个端口同时只能跑一个实例（`PortInUseException`）。

## 4. 快速开始

1. `git clone` 项目后，用 IDEA 打开**根目录**（Maven 工程会自动导入，包含全部子模块）；
2. 找到要学的模块，直接运行它的启动类（例如 [`BaseApplication`](01-springboot-base/src/main/java/com/hyao/base/BaseApplication.java)），控制台出现 `Tomcat started on port(s): xxxx` 即启动成功；
3. 03 模块需要额外准备数据库：先执行 [`script/1.sql`](03-springboot-mybatis/src/main/resources/script/1.sql)，必要时改 `application.yaml` 里的账号密码。

```bash
# 01：原生返回
curl http://localhost:8081/api/hello
# > Hello, World!

# 02：统一响应包装（注意业务异常也是 200，靠 body 里的 code 判断）
curl http://localhost:8082/api/hello
# > {"code":"SUC0000","message":"操作成功","data":"Hello World!"}

# 03：MyBatis 查询
curl -X POST http://localhost:8083/user/id -H 'Content-Type: application/json' -d '{"id":1}'
```

## 5. 项目结构

```
SpringBoot-Learning
├── pom.xml                          # 父 POM：聚合模块 + 统一版本 + 公共依赖
├── 00-springboot-common             # 公共库：响应包装 / 全局异常 / 统一日志（自动配置，无启动类）
├── 01-springboot-base               # 01 基础：MVC / REST 控制器
├── 02-springboot-advice             # 02 公共能力的示例（依赖 00）
├── 03-springboot-mybatis            # 03 MyBatis + Druid（依赖 00）
└── 04-springboot-mybatis-plus       # 04 预留（依赖 00，暂未纳入 modules）
```

## 6. 工程约定

### 6.1 父 POM 做了什么

父工程 `com.hyao:SpringBoot-Learning:1.0.0-SNAPSHOT` 的 `packaging` 为 `pom`，本身不打可执行包，只承担三件事：

1. **模块聚合**：`<modules>` 里列出所有子模块，新增模块在这里追加一行即可，父工程会自动把它纳入统一管理；
2. **统一版本（BOM）**：`scope=import` 导入 `spring-boot-dependencies`，Spring Boot 全家桶的版本一次性约定好，子模块引 Spring Boot 相关依赖**不用写 `<version>`**。BOM 管不到的要在 `dependencyManagement` 里单独补：

   | 依赖 | 版本来源 | 为什么 |
   | --- | --- | --- |
   | `mybatis-spring-boot-starter` | 父 POM 属性 `mybatis-spring-boot.version` | 第三方，BOM 里没有 |
   | `druid-spring-boot-starter` | 父 POM 属性 `druid-spring-boot.version` | 第三方，BOM 里没有 |
   | `00-springboot-common` | `${project.version}` | **自家模块**，跟工程版本走，不能用 `spring.boot.version` |

   注意 `dependencyManagement` 只统一**版本**；「要不要依赖」由各模块在自己的 `<dependencies>` 里决定——所以 00 只被 02/03/04 依赖，01 故意不依赖。

3. **公共依赖**：`<dependencies>` 里声明的依赖会被**所有子模块继承**：

   | 依赖 | scope | 作用 |
   | --- | --- | --- |
   | `spring-boot-starter-web` | compile | Web 起步依赖（内嵌 Tomcat + Spring MVC + Jackson） |
   | `spring-boot-devtools` | runtime / optional | 热部署，代码变更后自动重启应用上下文 |
   | `spring-boot-configuration-processor` | compile / optional | 编译期生成配置元数据，让 IDE 对自定义配置项做提示 |
   | `lombok` | provided / optional | 编译期生成 getter/setter 等样板代码，不参与打包 |
   | `commons-io` | compile | 通用 IO 工具类。**不在 BOM 管理范围内**，必须显式写 `<version>` |
   | `commons-lang3` | compile | 字符串工具类（`StringUtils`）。**在 BOM 管理范围内**，不写 `<version>` |

   带 `optional=true` 的三个依赖（`devtools`、`configuration-processor`、`lombok`）不会被传递给「依赖本项目的其他项目」；`devtools` 用 `runtime`（只在运行时用）、`lombok` 用 `provided`（只在编译期用）。

### 6.2 接口响应约定（自 02 模块起）

> **业务成功还是失败，一律看响应体里的 `code`**——`SUC0000` 表示成功，其他值对应 `SysErrorCode`，或业务模块自己实现 `IErrorCode` 的错误码（如 03 的 `DBE0001`）。**业务异常时 HTTP 状态码也是 `200`**。

请求 / 参数层面的异常会保留真实的 HTTP 状态码，但响应体同样是统一的 [`Result`](00-springboot-common/src/main/java/com/hyao/common/model/Result.java)，错误码按场景细分：

| code | 含义 | HTTP 状态码 |
| --- | --- | --- |
| `SYS0001` | 系统内部异常 | `500`（未知异常兜底时为 `200`） |
| `SYS0002` | 请求方法不支持（如接口只收 POST 却用了 GET） | `405`（带 `Allow` 头） |
| `SYS0003` | 接口不存在（URL 找不到） | `404` |
| `SYS0004` | 参数校验失败（`@Valid` 不通过、缺参数、请求体解析失败） | `400` |
| `SYS0005` | 参数类型不匹配（如 `pageSize=abc`） | `400` |
| `SYS0006` | 其余 4xx 兜底 | 原始 4xx |

后端用 `spring-boot-starter-validation`（Bean Validation）做声明式校验，校验失败统一 `400 + SYS0004`（字段错误拼成 `字段: 提示`）。后续模块都遵循这个约定，细节见 [02 模块 README](02-springboot-advice/README.md) 与 [00 模块 README](00-springboot-common/README.md)。
