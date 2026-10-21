# 03-springboot-mybatis

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**，只覆盖本模块已有代码对应的知识点，内容按实际代码与实测结果核对过，仅供参考；**如与源码不一致，请以源码为准**。

## 介绍

- **MyBatis**：半自动 ORM 框架。SQL 由你写（注解或 XML），MyBatis 负责填参数、把结果集映射成对象；不像 JPA/Hibernate 那样替你生成 SQL——SQL 可控、可调优是它最大的优点，代价是 SQL 要自己写。
- **Druid**：数据库连接池。维护一组物理连接循环复用，省掉每次请求的 TCP 握手与认证；同时提供监控统计、慢 SQL 记录、SQL 防火墙和内置监控页。
- **`mybatis-spring-boot-starter`**：把 MyBatis 接进 Spring Boot，具体做四件事：
  - 自动探测存在的 `DataSource`
  - 将使用 `SqlSessionFactoryBean` 创建并注册一个 `SqlSessionFactory` 的实例，并将探测到的 `DataSource` 作为数据源
  - 将创建并注册一个从 `SqlSessionFactory` 中得到的 `SqlSessionTemplate` 的实例
  - 自动扫描你的 mapper，将他们与 `SqlSessionTemplate` 相关联，并将它们注册到 Spring 的环境（context）中去，这样它们就可以被注入到你的 bean 中
- **`druid-spring-boot-starter`**：把 Druid 接进 Spring Boot——向容器注册 `DruidDataSourceWrapper`（本质是 `DruidDataSource`）作为数据源，并装配监控/过滤器（`stat`、`wall`、`slf4j`）。

## 1. 学习目标

分两半，缺一不可：

1. **集成原理**：Spring Boot 怎样把 MyBatis 和 Druid 装到一起——依赖怎么组合、属性怎么绑定、自动配置按什么顺序和条件装配、容器里的 `DataSource` 为什么一定是 Druid、一次 Mapper 调用怎样穿过 Spring 的事务与异常体系落到 Druid 的池子上。
2. **用业务场景带出 MyBatis 的用法和常用特性**：用「订单 + 库存」这个企业里最常见的场景，把注解 Mapper、XML Mapper、动态 SQL、`resultMap` 一对多、批量插入、游标分页、枚举 TypeHandler、分布式主键、事务与库存扣减串起来。

## 2. 学习清单

### 2.1 业务场景（特性 → 业务问题）

| 状态 | 优先级 | 业务场景 | 覆盖的 MyBatis 特性 | 涉及的集成点 |
| --- | --- | --- | --- | --- |
| ✅ | ★★★ | **订单创建**（`order_info` + `order_item` + 扣 `sku_stock`） | 雪花分布式主键、`<foreach>` 批量插明细、条件更新扣库存（影响行数判断） | `@Transactional` 跨三表、异常翻译（库存不足 / 下架 → 业务码）、DTO + `@Valid` |
| ✅ | ★★★ | **订单详情**（订单 + 明细） | `<resultMap>` + `<collection>` 一对多、`<id>` 聚合、`resultType` 实体 + 驼峰、枚举 TypeHandler | 无（纯 MyBatis 侧） |
| ✅ | ★★★ | **订单列表条件分页** | `<where>` + `<if>` 动态条件、`LIMIT` 参数化、游标分页（多查一条）、`resultType="map"`（列名保持下划线） | DTO + `@Valid`、SQL 日志（统一格式 + traceId） |
| ✅ | ★★☆ | **订单状态流转 / 取消** | `<set>` + `<if>` 选择性更新、`WHERE status = 前置状态` 保证幂等、影响行数判断 | 事务、取消后回补库存 |
| ✅ | ★★☆ | **库存扣减**（防超卖） | 条件更新 `UPDATE ... WHERE stock >= n`（影响行数 0 = 库存不足） | 事务、`@Transactional` 回滚 |
| ✅ | ★★☆ | **报表统计**（按天 / 状态聚合） | `<choose>/<when>/<otherwise>` 走互斥 SQL、`GROUP BY DATE()`、`<if>` 过滤、聚合结果 `resultType="map"` | `@AssertTrue` 交叉校验、`@JsonFormat` 日期入参、`resultType="map"` 的列别名 |
| ✅ | ★★☆ | **数据字典 + 二级缓存** | `<cache/>` 二级缓存、`useCache` / `flushCache`、`readOnly`、命中率日志 | `cache-enabled` / `local-cache-scope`、缓存失效边界（同 namespace 才清） |
| 🚧 | ★★☆ | **多数据源**（订单库 / 商品库） | 多个 `SqlSessionFactory`、`@MapperScan(sqlSessionTemplateRef)` | 待完成：`entity`/`mapper`/`model` 已按 `order`/`product`/`dict` 分包，XML 也已分目录 |

### 2.2 集成原理

1. **依赖组合**：先弄清三个 starter 各带来什么——`mybatis-spring-boot-starter` 带 MyBatis 本体、`mybatis-spring`（把 MyBatis 接进 Spring 的桥）和它的 autoconfigure；`druid-spring-boot-starter` 带 Druid 本体和 autoconfigure；版本都由父 POM 统一管。
2. **配置绑定**：`spring.datasource.*` 落到 `DataSourceProperties`，再被 `DruidDataSourceWrapper.afterPropertiesSet()` 取过去 set 到池子上；`spring.datasource.druid.*` 直接通过 `@ConfigurationProperties` 绑到 `DruidDataSource`；`mybatis.*` 落到 `MybatisProperties`；`app.id.*` 落到本模块的 `SnowflakeProperties`（见 §3.2）。
3. **自动配置装配**：`DruidDataSourceAutoConfigure` 靠 `@AutoConfigureBefore(DataSourceAutoConfiguration)` + `@ConditionalOnMissingBean` 抢在 Boot 原生数据源前面注册 Druid，Hikari 因此退让；`MybatisAutoConfiguration` 再通过 `@ConditionalOnSingleCandidate(DataSource.class)` 拿到容器里唯一的数据源，创建 `SqlSessionFactory` / `SqlSessionTemplate`（见 §4.3.1）。
4. **Mapper 扫描与代理**：`@MapperScan` 注册 `MapperScannerConfigurer`，为每个接口生成 `MapperFactoryBean`，`getObject()` 返回 JDK 动态代理；它与自动 `@Mapper` 扫描器二选一（见 §4.1）。
5. **运行时串联**：一次调用走 MapperProxy → `SqlSessionTemplate` → `SqlSessionUtils` → `SpringManagedTransaction` → `DataSourceUtils` → `DruidDataSource`；事务、异常翻译、SQL 日志都挂在这条链上（见 §4.3.2、§4.2.9、§3.2.6）。
6. **横切配套**：参数校验（00 的 Bean Validation，失败 `400 + SYS0004`）、统一响应 / 异常、traceId（见 §4.2.10）。
7. **缓存层级**：一级缓存挂在 `SqlSession`（Spring 下即一次事务/一次 Mapper 调用），二级缓存挂在 `SqlSessionFactory` + namespace 的 `<cache/>` 上，两者开关和失效规则不同（见 §4.2.12）。

> 串起来看就是一句话：**配置决定 Bean，Bean 的注册顺序决定容器里只有一个 Druid 数据源；MyBatis 只依赖 `javax.sql.DataSource` 接口，所以「换成哪个池子」对 Mapper 代码完全透明。**

## 3. 依赖与配置

### 3.1 依赖

```xml
<dependencies>
    <!-- 公共库 -->
    <dependency>
        <groupId>com.hyao.common</groupId>
        <artifactId>00-springboot-common</artifactId>
    </dependency>
    <!-- mybatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
    </dependency>
    <!-- mysql驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- Druid 连接池 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3.2 配置

#### 3.2.1 Spring 数据源（`spring.datasource.*`）

spring数据源配置以`spring.datasource`作为前缀，配置项参考`org.springframework.boot.autoconfigure.jdbc.DataSourceProperties`：

| 配置                   | 说明                                                                         |
| ---------------------- | ---------------------------------------------------------------------------- |
| `url`                  | JDBC 连接地址                                                                |
| `username`             | 数据库登录用户名                                                             |
| `password`             | 数据库登录密码                                                               |
| `driver-class-name`    | JDBC驱动类的全限定名，默认根据`url`自动检测，一般可以省略                    |
| `type`                 | 连接池实现类                                                                 |
| `generate-unique-name` | 是否为此数据源生成一个随机名称，默认`true`                                   |
| `name`                 | 数据源名称，用于 JNDI / 日志标识。`generate-unique-name`为`false`时生效      |
| `jndi-name`            | 走JNDI获取数据源时使用（应用服务器场景），此配置会忽略`url`/`username`等配置 |

#### 3.2.2 Druid 连接池（`spring.datasource.druid.*`）

**池容量与连接获取**

| 配置                                    | 说明                                                             |
| --------------------------------------- | ---------------------------------------------------------------- |
| `initial-size`                          | 初始化时建立的物理连接数                                         |
| `min-idle`                              | 最小空闲连接数                                                   |
| `max-active`                            | 最大活跃连接数（池上限）                                         |
| `max-wait`                              | 获取连接的最大等待时间（ms），超时抛 `GetConnectionTimeoutException` |
| `async-init`                            | 是否异步初始化连接池                                             |
| `fail-fast`                             | 初始化失败时是否直接失败                                         |
| `connection-error-retry-attempts`       | 创建连接失败后的重试次数                                         |
| `default-auto-commit`                   | 默认是否自动提交                                                 |
| `default-read-only`                     | 默认是否只读                                                     |
| `default-transaction-isolation`         | 默认事务隔离级别                                                 |

**空闲检测与探活**

| 配置                                    | 说明                                                     |
| --------------------------------------- | -------------------------------------------------------- |
| `time-between-eviction-runs-millis`     | 空闲连接检测线程的运行间隔（ms）                         |
| `min-evictable-idle-time-millis`        | 连接最小生存时间（ms），超过才可被回收                   |
| `keep-alive`                            | 是否对空闲连接保活                                       |
| `keep-alive-between-time-millis`        | 保活间隔（ms）                                           |
| `validation-query`                      | 探测连接是否有效的 SQL（如 `SELECT 1`）                   |
| `test-while-idle`                       | 空闲时检测连接是否有效                                   |
| `test-on-borrow` / `test-on-return`     | 借出 / 归还时检测（每次多一次往返，影响性能）             |
| `validation-query-timeout`              | 探活超时（秒）                                           |

**泄漏检测与语句缓存**

| 配置                                                | 说明                                             |
| --------------------------------------------------- | ------------------------------------------------ |
| `remove-abandoned`                                  | 是否回收"借出后长时间未归还"的连接（连接泄漏兜底） |
| `remove-abandoned-timeout`                          | 借出多久未归还算泄漏（秒）                       |
| `log-abandoned`                                     | 是否打印泄漏连接的堆栈                           |
| `pool-prepared-statements`                          | 是否缓存 `PreparedStatement`                     |
| `max-pool-prepared-statement-per-connection-size`   | 每个连接缓存的 PS 上限                           |

**监控与扩展**

| 配置                                                                                                         | 说明                                    |
| ------------------------------------------------------------------------------------------------------------ | --------------------------------------- |
| `filters`                                                                                                    | 过滤器链，常用 `stat,wall,slf4j`        |
| `filter.stat.enabled` / `filter.stat.slow-sql-millis` / `filter.stat.log-slow-sql` / `filter.stat.merge-sql`  | 统计与慢 SQL 记录                       |
| `filter.wall.enabled` / `filter.wall.config.multi-statement-allow` 等                                         | SQL 防火墙（拦截高危 SQL）              |
| `stat-view-servlet.enabled` / `stat-view-servlet.url-pattern` / `stat-view-servlet.login-username` / `stat-view-servlet.login-password` / `stat-view-servlet.reset-enable` | 内置监控页（如 `/druid/*`），生产要改账号密码 |
| `web-stat-filter.enabled` / `web-stat-filter.url-pattern` / `web-stat-filter.exclusions`                      | Web 请求统计                            |
| `aop-patterns`                                                                                               | 需要统计的 Spring Bean 切点（如 `com.hyao.*.service.*`） |
| `connection-properties`                                                                                      | 透传给驱动的连接属性                    |
| `use-global-data-source-stat`                                                                                | 多数据源时是否合并统计                  |

#### 3.2.3 MyBatis（`mybatis.*`）

配置项参考 `org.mybatis.spring.boot.autoconfigure.MybatisProperties`：

| 配置                              | 说明                                                                 |
| --------------------------------- | -------------------------------------------------------------------- |
| `mapper-locations`                | XML mapper 位置，本模块为 `classpath:mapper/*/*.mapper.xml`（按 `order`/`product`/`dict` 分目录），**不配 XML 不会被加载** |
| `type-aliases-package`            | 扫描该包并注册短类名别名（`resultType="OrderInfo"`）                  |
| `type-handlers-package`           | 扫描该包并注册自定义 TypeHandler（本模块为 `com.hyao.mybatis.common.handler`） |
| `config-location`                 | 原生 `mybatis-config.xml` 的位置                                     |
| `configuration-properties`        | 传给 `config-location` 的变量（在 XML 里用 `${xxx}` 引用）            |
| `check-config-location`           | 启动时是否校验 `config-location` 存在                                |
| `executor-type`                   | 默认执行器：`SIMPLE`（默认）/ `REUSE` / `BATCH`                      |
| `lazy-initialization`             | `SqlSessionFactory` 是否延迟初始化                                   |
| `inject-sql-session-on-mapper-scan` | 是否向 Mapper 注入 `SqlSessionTemplate`（一般不用改）               |

> `config-location` 与 `configuration.*` **不能同时配置**（会抛 `IllegalStateException`）：前者交给原生 XML，后者走 Spring Boot 的属性绑定。

#### 3.2.4 MyBatis `Configuration`（`mybatis.configuration.*`）

对应 `org.apache.ibatis.session.Configuration` 的常用属性：

| 配置                                    | 说明                                                                 |
| --------------------------------------- | -------------------------------------------------------------------- |
| `map-underscore-to-camel-case`          | 下划线列名 → 驼峰属性（`user_id` → `userId`），**只对 POJO 生效**    |
| `cache-enabled`                         | 二级缓存总开关（还要 namespace 里写 `<cache/>` 才生效）               |
| `local-cache-scope`                     | 一级缓存范围：`SESSION`（默认，一个 SqlSession 内有效）/ `STATEMENT`（语句级，等于关掉） |
| `lazy-loading-enabled`                  | 是否开启延迟加载                                                     |
| `aggressive-lazy-loading`               | 是否一次性加载所有懒加载属性                                         |
| `default-statement-timeout`             | 语句超时（秒），0 表示不限                                           |
| `default-fetch-size`                    | 默认 `fetchSize`                                                     |
| `call-setters-on-nulls`                 | 值为 null 时是否调用 setter（返回 `map` 时决定 key 是否保留）         |
| `jdbc-type-for-null`                    | null 值使用的 `JdbcType`（如 `NULL`）                                 |
| `use-generated-keys`                    | 插入时是否默认回填自增主键                                           |
| `default-executor-type`                 | 同 `mybatis.executor-type`                                           |
| `default-enum-type-handler`             | 默认枚举 TypeHandler（如 `EnumTypeHandler`）                          |
| `auto-mapping-behavior`                 | 自动映射：`NONE` / `PARTIAL`（默认）/ `FULL`                          |
| `auto-mapping-unknown-column-behavior`  | 未知列处理：`NONE`（默认）/ `WARNING` / `FAILING`                     |
| `default-result-set-type`               | `FORWARD_ONLY`（默认）/ `SCROLL_INSENSITIVE` / `SCROLL_SENSITIVE`     |
| `multiple-result-sets-enabled`          | 是否允许多结果集                                                     |
| `log-impl` / `log-prefix`               | 日志实现与前缀（建议用 slf4j + `logging.level`，别用 `StdOutImpl`）    |
| `shrink-whitespaces-in-sql`             | 是否压缩 SQL 中的多余空白                                             |
| `use-column-label`                      | 用列标签而不是列名作为结果列名                                       |
| `use-actual-param-name`                 | 是否使用真实参数名（需 `-parameters` 编译参数）                       |

#### 3.2.5 自定义配置（`app.id.*`）：雪花 ID 的机器位

```yaml
app:
  id:
    worker-id: ${WORKER_ID:0}          # 0~31，集群里每个实例必须不同
    datacenter-id: ${DATACENTER_ID:0}  # 0~31
```

对应 [`SnowflakeProperties`](src/main/java/com/hyao/mybatis/config/SnowflakeProperties.java)（`@ConfigurationProperties(prefix = "app.id")`）+ [`SnowflakeConfig`](src/main/java/com/hyao/mybatis/config/SnowflakeConfig.java)（`@EnableConfigurationProperties` 后 `@Bean` 创建 `SnowflakeIdGenerator`）。

- 集群部署时通过环境变量（如 K8s 的 `POD_INDEX`）或启动参数给每个实例注入不同的 `worker-id`；
- 同一个 `workerId` + `datacenterId` 起两个实例会生成**重复订单号**（主键冲突）。

#### 3.2.6 日志与建表（Spring 侧）

| 配置                                                                     | 说明                                                     |
| ------------------------------------------------------------------------ | -------------------------------------------------------- |
| `logging.level.<mapper 包>`                                               | SQL 日志级别（`debug` 打印 SQL、`trace` 还打印结果集），MyBatis 按 **Mapper 方法**打日志 |
| `spring.sql.init.mode` / `spring.sql.init.schema-locations` / `spring.sql.init.data-locations` | 启动时执行建表 / 初始化数据脚本         |

### 3.3 建表脚本

**[`script/1.sql`](src/main/resources/script/1.sql)** —— 订单 + 库存域：

| 表 | 关键设计 |
| --- | --- |
| `order_info` | **`id` 是字符串主键**（`varchar(32)`，雪花算法生成，同时就是订单号）；`status` tinyint + `cancelled_at`（取消标记）；索引 `idx_user_id(user_id,id)`、`idx_status_id(status,id)`、`idx_created_at` |
| `order_item` | `order_id` 是字符串（关联 `order_info.id`）；`sku_name` / `price` 保存**下单时的商品快照**；批量插入用 `<foreach>` |
| `sku_info` | 商品信息（名称 / 售价 / 上下架），创建订单时校验 |
| `sku_stock` | 库存 + `version` 乐观锁字段；扣减用条件更新 `stock >= n` |

脚本自带初始化数据：4 个 SKU（含 1 个下架）、对应库存（100/200/50/0）、2 条示例订单（1 条已支付 2 条明细、1 条待支付 1 条明细）。设计约定：不建外键、金额统一 `decimal(12,2)`、取消用 `status + cancelled_at`。

**[`script/2.sql`](src/main/resources/script/2.sql)** —— 数据字典域：

| 表 | 关键设计 |
| --- | --- |
| `dict_item` | 自增主键 `id`；唯一键 `uk_type_code(dict_type, dict_code)` 保证同类型下编码不重复；索引 `idx_type_sort(dict_type, sort)` 支撑「按类型查 + 排序」；`status` 1 启用 / 0 停用，查询只取启用的 |

自带两组字典数据：`order_status`（0~4）、`sku_status`（0 下架 / 1 上架），用 `INSERT IGNORE` 写，可重复执行。

> 执行前确认库名与 `spring.datasource.url` 里的一致。

## 4. 核心实现

### 4.1 启动配置

**① 启动类上的 `@MapperScan`（启动 Mapper 扫描的必要注解）**

```java
@SpringBootApplication
@MapperScan("com.hyao.mybatis.mapper")   // 统一扫描，Mapper 接口不用加 @Mapper
public class MybatisApplication { ... }
```

- 作用：把指定包下所有接口注册成 Mapper Bean，之后才能 `@Autowired OrderInfoMapper`；
- 机制：`MapperScannerConfigurer` 为每个接口注册 `MapperFactoryBean`，`getObject()` 返回 JDK 动态代理（`MapperProxy`）——**所以 Mapper 接口不需要实现类**；
- 替代方式：逐个接口加 `@Mapper`，由 MyBatis 自动配置里的 `AutoConfiguredMapperScannerRegistrar` 扫描；它带 `@ConditionalOnMissingBean(MapperScannerConfigurer.class)`，所以两种方式**二选一**。

**② 配置项三件套（不配不报错，但功能不生效）**

```yaml
mybatis:
  mapper-locations: classpath:mapper/*/*.mapper.xml           # XML mapper 才会被加载（按 order/product/dict 分目录）
  type-aliases-package: com.hyao.mybatis.entity               # resultType 可用短类名
  type-handlers-package: com.hyao.mybatis.common.handler      # 枚举 TypeHandler 才会被注册
  configuration:
    map-underscore-to-camel-case: true                        # 下划线列名 → 驼峰属性
```

- 漏 `mapper-locations` → XML 不加载，调用报 `Invalid bound statement`；漏 `type-aliases-package` → 短类名找不到；漏 `type-handlers-package` → 枚举字段转不了；
- 完整的 `mybatis.*` / `mybatis.configuration.*` 配置项见 §3.2.3、§3.2.4。

### 4.2 具体实现

#### 4.2.1 注解式 Mapper（`SkuStockMapper`，最简示例）

`sku_stock` 只有 4 列、纯单表，最适合第一次写注解 Mapper：

```java
public interface SkuStockMapper {

    @Select("SELECT sku_id, stock, version FROM `sku_stock` WHERE sku_id = #{skuId}")
    SkuStock queryBySkuId(@Param("skuId") String skuId);

    /** 条件更新扣库存：影响行数 1 = 成功；0 = 库存不足或 SKU 不存在 */
    @Update("UPDATE `sku_stock` SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND stock >= #{quantity}")
    int deduck(@Param("skuId") String skuId, @Param("quantity") int quantity);

    /** 取消订单回补库存 */
    @Update("UPDATE `sku_stock` SET stock = stock + #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId}")
    int restore(@Param("skuId") String skuId, @Param("quantity") int quantity);
}
```

- **`@Param`**：多参数必须写，否则只能用 `#{arg0}`/`#{param1}`；
- **`#{}` 是预编译占位符**，天然防注入；
- **返回值是影响行数**：`deduck` 返回 0 时业务层抛 `SKU0003`（库存不足）；
- **条件更新防超卖**：`WHERE stock >= #{quantity}` 让并发下的判断和扣减在一条 SQL 里原子完成，不会扣成负数。

#### 4.2.2 注解里的动态 SQL（`SkuInfoMapper`）

注解里也能写动态 SQL，把整段 SQL 包进 `<script>` 即可：

```java
@Select({"<script>",
        "SELECT sku_id, sku_name, price, status FROM `sku_info` WHERE sku_id IN",
        "<foreach collection='skuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"})
List<SkuInfo> queryBySkuIds(@Param("skuIds") List<String> skuIds);
```

- `OrderService.createOrder` 里先用它**一次 IN 查询**把商品查全，避免 N 次单查；
- 返回值 `SkuInfo.status` 是 `SkuStatus` 枚举，靠 TypeHandler 转换（见 §4.2.7）；
- 注解里超过两三行的 SQL 建议放 XML，可读性更好。

#### 4.2.3 XML：`<foreach>` 批量插入（`OrderItemMapper`）

```xml
<insert id="batchInsert" parameterType="OrderItem" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO `order_item`
        (order_id, sku_id, sku_name, price, quantity, amount)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.orderId}, #{item.skuId}, #{item.skuName}, #{item.price}, #{item.quantity}, #{item.amount})
    </foreach>
</insert>
```

```java
int batchInsert(List<OrderItem> items);   // 没写 @Param，集合默认叫 list
```

- **`collection="list"`**：接口参数是 `List` 且没有 `@Param` 时，MyBatis 把它放在 `list`（和 `collection`）键下；写了 `@Param("items")` 就必须用 `collection="items"`；
- **`useGeneratedKeys` 在 `<foreach>` 批量插入里不要指望回填每一行的自增 id**（MySQL 只回传首个/最后一批 key），本模块明细 id 不需要回填；
- 每行是一组 `(...)`，`separator=","` 负责拼逗号。

#### 4.2.4 XML：`<resultMap>` + `<collection>` 一对多（`OrderInfoMapper.queryDetail`）

```xml
<resultMap id="orderDetailMap" type="OrderInfo">
    <id column="id" property="id"/>
    <result column="user_id" property="userId"/>
    <result column="status" property="status"/>
    <result column="total_amount" property="totalAmount"/>
    <result column="pay_amount" property="payAmount"/>
    <result column="receiver_name" property="receiverName"/>
    <result column="receiver_phone" property="receiverPhone"/>
    <result column="receiver_address" property="receiverAddress"/>
    <result column="remark" property="remark"/>
    <result column="created_at" property="createdAt"/>
    <result column="updated_at" property="updatedAt"/>
    <result column="cancelled_at" property="cancelledAt"/>
    <collection property="items" ofType="OrderItem">
        <id column="item_id" property="id"/>
        <result column="sku_id" property="skuId"/>
        <result column="sku_name" property="skuName"/>
        <result column="price" property="price"/>
        <result column="quantity" property="quantity"/>
        <result column="amount" property="amount"/>
    </collection>
</resultMap>

<select id="queryDetail" resultMap="orderDetailMap">
    SELECT o.id, o.user_id, o.status, o.total_amount, o.pay_amount,
           o.receiver_name, o.receiver_phone, o.receiver_address, o.remark,
           o.created_at, o.updated_at, o.cancelled_at,
           i.id AS item_id, i.sku_id, i.sku_name, i.price, i.quantity, i.amount
    FROM `order_info` o
    LEFT JOIN `order_item` i ON i.order_id = o.id
    WHERE o.id = #{orderId}
    ORDER BY i.id
</select>
```

要点：

- **主表的 `<id>` 不能省**：MyBatis 靠它判断「是不是同一条订单」，否则明细会聚合错；
- **明细主键要起别名**：`i.id AS item_id`，避免与主表 `id` 冲突；
- **`LEFT JOIN`**：没有明细的订单也能查出来（`items` 为空数组）；
- `OrderInfo` 上要有一个 `List<OrderItem> items` 字段（非表字段，仅用于聚合）；
- 这条 SQL **只用于详情**——列表分页不能 join 明细，否则行数被放大，分页就错了。

#### 4.2.5 XML：动态条件 + 游标分页（`pageQuery`）

```xml
<select id="pageQuery" parameterType="map" resultType="map">
    SELECT id, user_id, status, total_amount, pay_amount,
           receiver_name, receiver_phone, receiver_address
    FROM `order_info`
    <where>
        <if test="lastId != null and lastId != ''">id &lt;= #{lastId}</if>
        <if test="userId != null and userId != ''">AND user_id = #{userId}</if>
        <if test="status != null">AND status = #{status}</if>
        <if test="minAmount != null">AND total_amount &gt;= #{minAmount}</if>
        <if test="maxAmount != null">AND total_amount &lt;= #{maxAmount}</if>
        <if test="createdFrom != null">AND created_at &gt;= #{createdFrom}</if>
        <if test="createdTo != null">AND created_at &lt;= #{createdTo}</if>
    </where>
    ORDER BY id DESC
    LIMIT #{limit}
</select>
```

- `<where>` 自动去掉开头的 `AND`/`OR`，所以 `<if>` 里统一写 `AND ...`；
- **游标语义（本模块是「包含式」）**：`OrderService` 先 `limit = pageSize + 1` 多查一条；如果查回来 `pageSize + 1` 条，就把**多出来的那条**（下一页第一条）的 id 作为 `nextCursor`，下次请求带 `lastId = nextCursor`，条件 `id <= lastId` 正好从它开始——所以翻页**不会重复也不会漏**；
- 排序键必须唯一且可比较（雪花 ID 定长且单调）；
- 索引命中：`idx_status_id(status, id)`、`idx_user_id(user_id, id)`、`idx_created_at`；
- **`resultType="map"` 时列名保持数据库原名**（`user_id`、`total_amount`），`status` 是原始数字——驼峰映射和 TypeHandler 都不作用于 Map（实测见 §5）。

#### 4.2.6 XML：状态更新（`updatePaid` / `updateDelivered` / `updateCancelled`）

```xml
<update id="updatePaid" parameterType="OrderInfo">
    UPDATE `order_info`
    SET status = 1, pay_amount = #{payAmount}
    WHERE id = #{id} AND status = 0          <!-- 只有待支付才能支付 -->
</update>

<update id="updateDelivered" parameterType="string">
    UPDATE `order_info`
    SET status = 2
    WHERE id = #{id} AND status = 1          <!-- 只有已支付才能发货 -->
</update>

<update id="updateCancelled" parameterType="OrderInfo">
    UPDATE `order_info`
    <set>
        status = 4,
        cancelled_at = #{cancelledAt},
        <if test="remark != null and remark != ''">remark = #{remark},</if>
    </set>
    WHERE id = #{id} AND status IN (0, 1)    <!-- 待支付 / 已支付可取消 -->
</update>
```

- **状态机写在 `WHERE` 里**：影响行数 1 才成功，0 说明状态不对 → `OrderService` 抛 `ORD0005`（支付/发货）或 `ORD0006`（取消）；
- 这样天然幂等：重复支付第二次影响行数 0，不会重复扣款；重复取消同理；
- `<set>` + `<if>` 演示「只更新传了的字段」（备注），并自动去掉末尾逗号；
- `cancelOrder` 上加了 `@Transactional`：**取消成功后查明细并回补库存**，两步在同一事务里。

#### 4.2.7 枚举 ↔ 整型字段：自定义 TypeHandler

```java
public interface BaseEnum { int getCode(); }

public enum OrderStatus implements BaseEnum {
    PENDING_PAYMENT(0, "待支付"), PAID(1, "已支付"), DELIVERED(2, "已发货"),
    COMPLETED(3, "已完成"), CANCELLED(4, "已取消");
    @JsonValue public int getCode() { return code; }     // 响应里输出数字
}
public enum SkuStatus implements BaseEnum { ON_SHELF(1, "上架"), OFF_SHELF(0, "下架"); }
```

```java
@MappedTypes(OrderStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class OrderStatusTypeHandler extends BaseEnumTypeHandler<OrderStatus> {
    public OrderStatusTypeHandler(Class<OrderStatus> type) { super(type); }
}
```

- 泛型基类 [`BaseEnumTypeHandler`](src/main/java/com/hyao/mybatis/common/handler/BaseEnumTypeHandler.java)：构造时把 `code → 枚举` 放进 Map；写库时 `ps.setInt(getCode())`，读库时反查；未知 code 抛 `BizException(BIZ_ENUM_ERROR)`（`BIZ0001`）；
- 子类必须提供 `Class<E>` 构造参数（MyBatis 用反射创建 TypeHandler 时会传目标类型）；
- **注册**：`mybatis.type-handlers-package: com.hyao.mybatis.common.handler`；
- **注意 `@JsonValue` 的影响**：`OrderDetailVO.orderStatus` 是枚举，响应里输出的是 **code（数字）**，不是 `"PAID"` 这样的枚举名。

#### 4.2.8 分布式主键：雪花算法（`SnowflakeIdGenerator`）

订单号不用数据库自增，而是应用层生成，集群多实例下仍保证全局唯一：

```java
public synchronized long nextId() {
    long currentTimestamp = System.currentTimeMillis();
    if (currentTimestamp < lastTimestamp) {                 // 时钟回拨
        long backward = lastTimestamp - currentTimestamp;
        if (backward <= MAX_BACKWARD_MS) { waitUntil(lastTimestamp); }
        else { throw new IllegalArgumentException("时钟回拨过大: " + backward + "ms"); }
    }
    if (currentTimestamp == lastTimestamp) {
        sequence = (sequence + 1) & SEQUENCE_MASK;          // 12 位序列
        if (sequence == 0L) { currentTimestamp = waitNextMillis(lastTimestamp); }
    } else { sequence = 0L; }
    lastTimestamp = currentTimestamp;
    return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)  // 41 位时间戳
            | (datacenterId << DATACENTER_ID_SHIFT)         // 5 位数据中心
            | (workerId << WORKER_ID_SHIFT)                 // 5 位机器
            | sequence;
}
```

- 布局：`41 位时间戳 + 5 位 datacenterId + 5 位 workerId + 12 位序列`；`EPOCH = 2020-01-01`；
- 输出是 `long`，`OrderService` 里 `String.valueOf(...)` 后作为订单号（`varchar(32)`），实测订单号形如 `900383869839081472`；
- **集群要求**：每个实例的 `workerId` 必须不同（见 §3.2.5），否则会生成重复 ID；
- 时钟回拨 ≤ 5ms 自旋等待，超过直接抛异常（宁可拒绝生成，也不要撞号）；
- 同毫秒序列用尽时自旋到下一毫秒。

#### 4.2.9 持久层异常（`DbExceptionHandler`）

[`DbExceptionHandler`](src/main/java/com/hyao/mybatis/common/exception/DbExceptionHandler.java)：

```java
@Slf4j
@Order(1)
@RestControllerAdvice
public class DbExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库操作异常", e);        // 细节（SQL、表名）只进日志
        return Result.fail(BizErrorCode.DB_ERROR.getCode(), BizErrorCode.DB_ERROR.getMessage());
    }
}
```

- MyBatis 的 `PersistenceException` 会被 `MyBatisExceptionTranslator` 转成 Spring 的 `DataAccessException`，所以这里能用 `@ExceptionHandler(DataAccessException.class)` 统一成 `DBE0001`；
- 加上注解`@Order(1)` ，保证它优先于 00 的 `@ExceptionHandler(Exception.class)` 兜底，`@Order` 应标在**类**上（或让类实现 `Ordered`）。

#### 4.2.10 请求参数校验（DTO + `@Valid`）

请求一律用 DTO + 约束注解，失败由 00 模块统一返回 `400 + SYS0004`：

```java
@Data
public class OrderCreateRequest {
    @NotBlank(message = "不能为空") private String userId;
    @NotBlank(message = "不能为空") @Size(max = 32) private String receiverName;
    @NotBlank(message = "不能为空") @Size(max = 255) private String receiverAddress;
    @NotBlank(message = "不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String receiverPhone;
    @Size(max = 255) private String remark;
    @NotEmpty(message = "至少一条订单明细") private List<OrderItemRequest> items;   // ⚠️ 少了 @Valid，见 §4.4
}

@Data
public class OrderItemRequest {
    @NotBlank(message = "不能为空") private String skuId;
    @NotNull(message = "不能为空") @Min(value = 1, message = "至少为 1") @Max(value = 999, message = "最多为 999")
    private Integer quantity;
}
```

```java
@PostMapping("/create")
public Map<String, Object> createOrder(@Valid @RequestBody OrderCreateRequest req) { ... }
```

- 校验失败响应形如 `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize分页大小不能为空","data":null}`（message 格式是「参数校验失败: 字段名+提示」）；
- **嵌套集合的校验需要 `@Valid` 级联**：`items` 上只有 `@NotEmpty`，没有 `@Valid`，所以明细里的 `quantity=0`、`skuId` 为空**不会**被拦截（见 §4.4）；
- `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 控制时间入参/出参格式（`OrderCancelRequest.cancelledAt`、`OrderDetailVO` 的时间字段）。

#### 4.2.11 XML：`<choose>` 走互斥 SQL（`OrderInfoMapper.statistics`）

报表要「按天」和「按状态」两种完全不同的 `SELECT`（查询列、`GROUP BY` 都不同），这不是「加不加条件」的区别，所以用 `<choose>/<when>/<otherwise>`（if / else if / else），`<if>` 只负责在选中分支里再加过滤：

```xml
<select id="statistics" resultType="map">
    <choose>
        <!-- 按天统计：DATE() 把时间截到天，再按天分组 -->
        <when test="dimension == 'day'">
            SELECT DATE(o.created_at) AS stat_key,
                   COUNT(*)           AS order_count,
                   SUM(o.total_amount) AS total_amount
            FROM `order_info` o
            WHERE o.created_at &gt;= #{beginDate}
              <!-- 半开区间：保证包含结束日 23:59:59，否则只算到当天 00:00 -->
              AND o.created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)
            <if test="status != null">
                AND o.status = #{status}
            </if>
            GROUP BY DATE(o.created_at)
            ORDER BY stat_key ASC
        </when>
        <!-- 其他值都按状态统计（兜底分支） -->
        <otherwise>
            SELECT o.status AS stat_key,
                   COUNT(*) AS order_count,
                   SUM(o.total_amount) AS total_amount
            FROM `order_info` o
            WHERE o.created_at &gt;= #{beginDate}
              AND o.created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)
            <if test="status != null">
                AND o.status = #{status}
            </if>
            GROUP BY o.status
            ORDER BY stat_key ASC
        </otherwise>
    </choose>
</select>
```

- **`<choose>` 只命中第一个为真的 `<when>`，都不满足走 `<otherwise>`**——所以 `dimension` 必须在 DTO 上用 `@Pattern(regexp = "day|status")` 卡死，否则传 `week` 会**静默**走「按状态」分支（实测 `200`，不会报错）；
- **`resultType="map"` 的 key 是列别名**：`stat_key` / `order_count` / `total_amount`，不是实体属性名；`Service` 按别名取出来再转 [`OrderStatisticsItemVO`](src/main/java/com/hyao/mybatis/model/order/OrderStatisticsItemVO.java)；
- **时间上界用半开区间** `>= beginDate AND < DATE_ADD(endDate, INTERVAL 1 DAY)`：`LocalDate` 绑参是当天 `00:00:00`，写成 `<= #{endDate}` 会把结束日的数据漏掉；
- `DATE()` / `DATE_ADD()` 是 **MySQL 方言**，换 H2 做集成测试要改写（见设计稿 §6.7）；
- `statusDesc` 只在 `dimension=status` 时由 `OrderStatus.ofCode(...)` 补上，`day` 维度是 `null`；
- 日期区间、90 天上限用两个 `@AssertTrue` 方法做**跨字段校验**，方法名会出现在错误信息里（实测 `参数校验失败: dateRangeValid开始日期不能晚于结束日期`）。

#### 4.2.12 二级缓存：`<cache/>`（`DictItemMapper`）

字典是「读多写少、允许短暂不一致」的数据，适合缓存。MyBatis 分两级：

| 级别 | 作用范围 | 开关 | 失效时机 |
| --- | --- | --- | --- |
| 一级缓存 | 同一个 `SqlSession`（Spring 下 = 同一次 `@Transactional` 或同一次 Mapper 调用） | 默认开 | 同一 session 内执行 update/insert/delete 即清空；`local-cache-scope: STATEMENT` 可关 |
| 二级缓存 | 同一个 `SqlSessionFactory`（整个应用），跨 session/跨请求共享 | 全局 `cache-enabled: true`（默认）+ 该 namespace 写 `<cache/>` | **同 namespace** 的 update/insert/delete 默认 `flushCache=true` 清空；跨 namespace 不会失效 |

```xml
<mapper namespace="com.hyao.mybatis.mapper.dict.DictItemMapper">

    <!-- LRU 淘汰；600s 强制过期；最多 512 个缓存条目；readOnly 直接返回缓存里的对象引用 -->
    <cache eviction="LRU" flushInterval="600000" size="512" readOnly="true" />

    <select id="queryByType" resultType="DictItem" useCache="true">
        SELECT id, dict_type, dict_code, dict_name, sort, status
        FROM `dict_item`
        WHERE dict_type = #{dictType}
          AND status = 1
        ORDER BY sort ASC
    </select>

    <update id="updateName" flushCache="true">
        UPDATE `dict_item` SET dict_name = #{dictName}
        WHERE dict_type = #{dictType} AND dict_code = #{dictCode}
    </update>
</mapper>
```

- **缓存 key** = `statementId + 参数`（+ `RowBounds`/分页参数），所以不同 `dictType` 是不同的条目；
- **`readOnly="true"`**：不反序列化拷贝，直接返回缓存里的**同一批对象**——最快，但调用方改了字段会污染缓存；本模块只读并立即转 VO，所以安全；
- **`flushCache` 默认值**：`select` 是 `useCache=true` / `flushCache=false`，`update`/`insert`/`delete` 是 `flushCache=true`；
- **验证方法（实测）**：连续两次 `POST /dict/list {"dictType":"order_status"}`，第一次日志有 `Preparing: SELECT ...`，第二次**没有 SQL**、多一行 `Cache Hit Ratio [com.hyao.mybatis.mapper.dict.DictItemMapper]: 0.5`；执行 `POST /dict/updateName` 再查，缓存被清、重新打 SQL 且拿到新值（见 §5）。

### 4.3 依赖之间怎么组合关联的

#### 4.3.1 Druid 怎么成为 Spring Boot 的数据源

两件事同时成立：

1. **Druid 的 starter 抢先把自己注册成 `DataSource`**：`DruidDataSourceAutoConfigure` 标了 `@AutoConfigureBefore(DataSourceAutoConfiguration.class)`，并注册 `@Bean(initMethod = "init")` 的 `DruidDataSourceWrapper`（`extends DruidDataSource`）；它在 `InitializingBean.afterPropertiesSet()` 里把 `spring.datasource.*` 的 url / username / password / driver 摘出来 set 到池子上，`init()` 再真正建池、按 `initial-size` 预建物理连接。
2. **Boot 原生的 `DataSourceAutoConfiguration` 退让**：它的 Hikari 数据源是 `@ConditionalOnMissingBean(DataSource.class)`，看到容器里已经有 Druid 就不再创建。

所以 yaml 里的 `spring.datasource.type: com.alibaba.druid.pool.DruidDataSource` 只是"明示用哪个实现"，真正让它生效的是 starter 的自动配置；这也解释了为什么 `spring.datasource.druid.*` 能直接绑定到池子上（`DruidDataSourceWrapper` 上标了 `@ConfigurationProperties("spring.datasource.druid")`）。

#### 4.3.2 MyBatis 怎么从 Druid 连接池中拿到连接

一句话：`MybatisAutoConfiguration` 拿容器里**唯一**的 `DataSource` Bean 去建 `SqlSessionFactory`（`@ConditionalOnSingleCandidate(DataSource.class)`），而那个 Bean 就是 §4.3.1 注册的 `DruidDataSourceWrapper`——MyBatis 只依赖 `javax.sql.DataSource` 接口，换成 Hikari/DBCP 也只是换 starter + 配置的事。

下面把一次 Mapper 调用从借连接到还连接的路径捋清楚（类名/方法名都在本项目实际版本上核对过：mybatis-spring 2.1.2 / mybatis 3.5.14 / druid 1.2.23 / spring-jdbc 5.3.31）：

| 层                       | 类                                                                      | 作用                                                                                                                                                                             |
| ------------------------ | ----------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Druid 自动配置           | `DruidDataSourceAutoConfigure` → `DruidDataSourceWrapper`               | 容器里那个 `DataSource` Bean（`extends DruidDataSource`，`InitializingBean.afterPropertiesSet()` 里 `init()` 池子）；有它之后 Boot 原生的 `DataSourceAutoConfiguration` 就退让了 |
| MyBatis 自动配置         | `MybatisAutoConfiguration`                                              | 创建 `SqlSessionFactory`（用 `SqlSessionFactoryBean`）和 `SqlSessionTemplate`，并把 `DataSource` 交给 `SqlSessionFactory`                                                        |
| Mapper 代理              | `MapperFactoryBean` / `MapperProxy`                                     | 接口没有实现类的原因                                                                                                                                                             |
| SqlSession 门面/生命周期 | `SqlSessionTemplate`（内部 `SqlSessionInterceptor`）、`SqlSessionUtils` | 负责拿/放 `SqlSession`（`getSqlSession` / `registerSessionHolder` / `closeSqlSession`）                                                                                          |
| 事务与连接的桥           | `SpringManagedTransaction`                                              | `getConnection()` 是取连接的入口                                                                                                                                                 |
| 连接同步                 | `DataSourceUtils` + `TransactionSynchronizationManager`                 | 决定「复用线程上已绑定的连接」还是「新借一条」                                                                                                                                   |
| 池本体                   | `DruidDataSource`                                                       | `getConnectionDirect → getConnectionInternal → takeLast` 取连接，`recycle` 归还                                                                                                  |

```
orderInfoMapper.queryDetail("ORD20261019000001")  // MapperProxy（JDK 动态代理）
└─ SqlSessionTemplate.selectOne(...)              // SqlSessionInterceptor 拦下
   └─ SqlSessionUtils.getSqlSession(factory)      // ★ 先看当前线程有没有绑定 SqlSession
      ├─ 有（在 Spring 事务里）→ 复用
      └─ 没有 → openSession()；若事务活跃则 registerSessionHolder 绑到当前线程
         └─ DefaultSqlSession → SimpleExecutor.query()
            └─ BaseExecutor.getConnection()
               └─ SpringManagedTransaction.getConnection()        // ★ mybatis-spring 的桥
                  └─ DataSourceUtils.getConnection(dataSource)    // ★ Spring 的事务同步入口
                     ├─ 线程上已绑定该 DataSource 的连接 → 复用（同一事务同一连接）
                     └─ 没有 → dataSource.getConnection()
                        └─ DruidDataSource.getConnection()
                           → getConnectionDirect() → getConnectionInternal() → takeLast()
                             ・池里有空闲 → 直接给  ・没到 max-active → 新建物理连接
                             ・已打满 → 在 max-wait 内等待，超时抛 GetConnectionTimeoutException
      ── 执行 SQL ──
   └─ SqlSessionUtils.closeSqlSession()
      └─ SpringManagedTransaction.close()
         └─ DataSourceUtils.releaseConnection()
            └─ DruidPooledConnection.close() → DruidDataSource.recycle()   // 归还池子，不是关 TCP
```

三个容易搞混的点：

- **两种"连接"不是一回事**：`DataSourceUtils.getConnection()` 返回的是 `DruidPooledConnection`（池化的逻辑连接），它的 `close()` 是 `recycle()` 回池，不是关物理连接；
- **有没有 `@Transactional` 行为不同**：没有事务时"借 → 用 → 还"（下一条 SQL 再借）；有事务时连接绑在线程上，**同一事务共用一条连接**，直到提交/回滚才释放——`OrderService.createOrder` 的 `@Transactional` 就让「扣库存 → 插订单 → 批插明细」这几条 SQL 共用一条连接，任何一步抛异常整体回滚；

**想自己验证**：在 `SpringManagedTransaction.getConnection()`、`DataSourceUtils.getConnection()`、`DruidDataSource.getConnectionInternal()` 上打断点看调用栈；或用 `DruidDataSource.getActiveCount()`（开 Druid 监控页）对比有无事务；MySQL 侧 `SHOW PROCESSLIST` 看物理连接数应在 `initial-size`/`min-idle` 量级。

### 4.4 容易踩坑的点

- `@MapperScan` 的包必须覆盖**所有** Mapper 接口；Mapper 接口**不要**同时写实现类，否则调用会绕过代理；
- `@MapperScan` 与 `@Mapper` 二选一：配了 `@MapperScan` 后 MyBatis 的自动 `@Mapper` 扫描器会退让（`@ConditionalOnMissingBean(MapperScannerConfigurer.class)`），**另一个包里的 `@Mapper` 接口不会被扫到**；
- 自定义 TypeHandler 必须被注册（`type-handlers-package` 指向 `com.hyao.mybatis.common.handler`，或子类上加 `@MappedTypes`），否则枚举字段转换失败；
- **`@Order` 要标在 advice 类上**：把 `@Order(1)` 标在方法上，对「多个 advice 之间谁先匹配」不起作用；要压过 00 的 `Exception` 兜底，必须加类级 `@Order`（或实现 `Ordered`）；
- **`resultType="map"` 不走驼峰、不走 TypeHandler**：列名保持下划线、`status` 是数字（实测见 §5）；
- **XML 里 `>`、`<` 要转义**（`&gt;`、`&lt;`），否则 XML 解析失败；
- **列表分页不要 join 明细**：一对多会放大行数，分页结果就错了（详情才用 join + `<collection>`）；
- **游标分页的 nextCursor 是「多查一条」的那条的 id**：SQL 用 `id <= #{lastId}`；如果改成 `id <`，要么把 nextCursor 换成当前页最后一条，否则会漏掉一条；
- **`<foreach>` 的 collection 名字**：`List` 参数没加 `@Param` 时叫 `list`（或 `collection`）；加了 `@Param("items")` 就必须写 `collection="items"`；
- **批量插入的 `useGeneratedKeys` 别指望回填每行 id**：MySQL 只回传首个/最后一批 key，多行 `<foreach>` 拿不到逐个 id；
- **嵌套 DTO 的约束需要 `@Valid` 级联**：如果`OrderCreateRequest.items` 上没有 `@Valid`，明细里的 `quantity`、`skuId` 约束不会触发；
- **雪花 ID 的集群要求**：同 `workerId + datacenterId` 的实例会生成重复订单号；时钟回拨超过 5ms 会抛异常（这是有意的保护）；
- `mybatis.configuration.log-impl: StdOutImpl` 会绕过 slf4j：SQL 日志不带 traceId、不进文件；改用 `logging.level.<mapper包>: debug`；
- `config-location` 与 `configuration.*` 同时配置会启动报错，二选一；
- Druid：`max-wait` 打满后抛 `GetConnectionTimeoutException`；`test-while-idle` 要配 `validation-query` 才有意义；`initial-size` 在库连不上时启动即失败；
- 枚举未知 `code` 会抛 `BizException(BIZ_ENUM_ERROR)`（`BIZ0001`），让脏数据早暴露。
- **二级缓存是「本地缓存」**：多实例部署时每个实例各存一份，A 实例更新字典后 B 实例不会失效；要跨实例一致就得换成 Redis 等集中式 `Cache` 实现；
- **只有同一 namespace 的写才会清缓存**：`DictItemMapper.updateName` 能清掉 `DictItemMapper` 的缓存；如果你在别的 mapper 里 `UPDATE dict_item`，或者直接用 SQL 客户端改库，缓存**不会**失效（只能等 `flushInterval` 过期或重启）；
- **`readOnly="true"` 返回共享实例**：别在 Service 里改缓存返回对象的字段，否则下次命中缓存就是脏数据；
- **`updateName` 不校验影响行数**：`dictCode` 不存在时影响 0 行，接口照样 `200`（实测 `Updates: 0`）；要报错得在 Service 里判断返回值；
- **`OrderStatus.ofCode` 可能返回 `null`**：库里 `status` 超出 0~4 时，`statistics` 里直接 `.getDesc()` 会 NPE → `500 + SYS0001`；
- **统计的 `<choose>` 不校验 `dimension`**：非法值会静默走 `<otherwise>`，DTO 上的 `@Pattern` 才是防线；
- **聚合/统计的列别名是契约**：`resultType="map"` 靠 `stat_key`、`order_count`、`total_amount` 取值，改 SQL 时别名要同步改 Service。

## 5. 快速验证

前置条件：

1. 启动本地 MySQL，创建库 `hyao`；
2. 执行 [`script/1.sql`](src/main/resources/script/1.sql)（建四张表 + 初始化 4 个 SKU、库存、2 条示例订单）和 [`script/2.sql`](src/main/resources/script/2.sql)（建 `dict_item` + 两组字典数据）；
3. 按需修改 `application.yaml` 的账号密码（`useSSL=true&requireSSL=true` 要求 MySQL 开了 SSL，本地没开会连不上，去掉 `requireSSL` 即可）；
4. IDEA 里运行 [`MybatisApplication`](src/main/java/com/hyao/mybatis/MybatisApplication.java) 的 `main`（端口 8083，同一端口同时只能跑一个实例）。

```bash
# 订单详情（示例订单，演示 resultMap + collection）
curl -X POST http://localhost:8083/order/detail \
  -H 'Content-Type: application/json' -d '{"orderId":"ORD20261019000001"}'

# 订单列表（动态条件 + 游标分页，pageSize 必填）
curl -X POST http://localhost:8083/order/page \
  -H 'Content-Type: application/json' -d '{"pageSize":2}'

# 创建订单（SKU001 ×1：扣库存 + 插订单 + 批插明细，同一事务）
curl -X POST http://localhost:8083/order/create -H 'Content-Type: application/json' -d '{
  "userId":"uid000000001","receiverName":"hyao","receiverPhone":"13800000000",
  "receiverAddress":"北京市海淀区中关村大街 1 号","remark":"示例",
  "items":[{"skuId":"SKU001","quantity":1}]}'

# 支付 / 发货 / 取消（把 <orderId> 换成真实订单号）
curl -X POST http://localhost:8083/order/pay \
  -H 'Content-Type: application/json' -d '{"orderId":"<orderId>","payAmount":399.00}'
curl -X POST http://localhost:8083/order/deliver \
  -H 'Content-Type: application/json' -d '{"orderId":"<orderId>"}'
curl -X POST http://localhost:8083/order/cancel \
  -H 'Content-Type: application/json' -d '{"orderId":"<orderId>","cancelledAt":"2026-10-20 22:30:00","remark":"不想要了"}'

# 报表统计（dimension=status / day；日期区间 ≤ 90 天）
curl -X POST http://localhost:8083/order/statistics \
  -H 'Content-Type: application/json' -d '{"beginDate":"2026-10-19","endDate":"2026-10-21","dimension":"status"}'
curl -X POST http://localhost:8083/order/statistics \
  -H 'Content-Type: application/json' -d '{"beginDate":"2026-10-19","endDate":"2026-10-21","dimension":"day"}'

# 数据字典（连发两次：第二次命中二级缓存，日志里没有 SQL）
curl -X POST http://localhost:8083/dict/list \
  -H 'Content-Type: application/json' -d '{"dictType":"order_status"}'
curl -X POST http://localhost:8083/dict/list \
  -H 'Content-Type: application/json' -d '{"dictType":"order_status"}'

# 改字典名（flushCache 清掉本 namespace 缓存，再查就是新值）
curl -X POST http://localhost:8083/dict/updateName \
  -H 'Content-Type: application/json' -d '{"dictType":"order_status","dictCode":"1","dictName":"已支付"}'
```

实测（本地 MySQL，2026-10-20 与 2026-10-21 两次运行结果；创建/支付/取消后的测试订单已清理，库存也回补到原值，字典名已还原）：

| 请求 | 结果 |
| --- | --- |
| `POST /order/detail` `{"orderId":"ORD20261019000001"}` | `200` + `{"code":"SUC0000","message":"操作成功","data":{"id":"ORD20261019000001","userId":"uid000000001","orderStatus":2,"totalAmount":528.00,"payAmount":528.00,"receiverName":"hyao","createdAt":"2026-10-19 21:27:31","cancelledAt":"1970-01-01 00:00:00","items":[{"id":1,"skuId":"SKU001","skuName":"机械键盘","price":399.00,"quantity":1,"amount":399.00},{"id":2,"skuId":"SKU002","skuName":"无线鼠标","price":129.00,"quantity":1,"amount":129.00}]}}` |
| `POST /order/page` `{"pageSize":2}` | `200` + `data.hasNextPage=true`、`data.nextCursor="900381547754946560"`、`data.rows` 2 条（key 为 `user_id`/`total_amount` 等下划线列名，`status` 是数字） |
| `POST /order/page` `{}` | `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize分页大小不能为空","data":null}` |
| `POST /order/create`（SKU001 ×1） | `200` + `{"code":"SUC0000","message":"操作成功","data":{"orderId":"900383869839081472"}}`（雪花 ID，每次不同） |
| `POST /order/create`（`items: []`） | `400` + `{"code":"SYS0004","message":"参数校验失败: items至少一条订单明细","data":null}` |
| `POST /order/create`（SKU004，已下架） | `200` + `{"code":"SKU0002","message":"商品已下架: SKU004","data":null}` |
| 新订单 `POST /order/detail` | `200` + `orderStatus:0`、`payAmount:0.00`、`items` 1 条 |
| `POST /order/pay`（`payAmount:399.00`） | `200` + `{"code":"SUC0000","message":"操作成功","data":null}`；再查详情 `orderStatus:1`、`payAmount:399.00` |
| `POST /order/cancel`（已支付） | `200` + `data:null`；库存回补（`sku_stock.stock` 回到下单前的值） |
| 重复 `POST /order/cancel` | `200` + `{"code":"ORD0006","message":"订单取消失败, 不存在待支付或已支付订单: <orderId>","data":null}` |
| 已取消订单再 `POST /order/pay` | `200` + `{"code":"ORD0005","message":"订单状态更新失败, 不存在待支付订单: <orderId>","data":null}` |
| `POST /order/statistics` `{"beginDate":"2026-10-19","endDate":"2026-10-21","dimension":"status"}` | `200` + `data` 3 项：`{"statKey":"0","statusDesc":"待支付","orderCount":2,"totalAmount":1956.00}`、`{"statKey":"2","statusDesc":"已发货","orderCount":1,"totalAmount":528.00}`、`{"statKey":"4","statusDesc":"已取消","orderCount":1,"totalAmount":657.00}` |
| 同上，`"dimension":"day"` | `200` + `data` 2 项：`{"statKey":"2026-10-19","statusDesc":null,"orderCount":2,"totalAmount":1827.00}`、`{"statKey":"2026-10-20","statusDesc":null,"orderCount":2,"totalAmount":1314.00}` |
| 同上，`"dimension":"status","status":0` | `200` + 只剩 `statKey:"0"` 那项（`<if>` 过滤生效） |
| `POST /order/statistics` `"dimension":"week"` | `400` + `{"code":"SYS0004","message":"参数校验失败: dimension只能是 day 或 status","data":null}` |
| `POST /order/statistics` `beginDate > endDate` | `400` + `{"code":"SYS0004","message":"参数校验失败: dateRangeValid开始日期不能晚于结束日期","data":null}` |
| `POST /order/statistics` 跨度 293 天 | `400` + `{"code":"SYS0004","message":"参数校验失败: rangeWithinLimit查询跨度不能超过 90 天","data":null}` |
| `POST /dict/list` `{"dictType":"order_status"}` | `200` + 5 条字典项（`dictCode` 0~4） |
| 紧接着再发一次同样的请求 | `200` + 同样 5 条；**日志里没有 SQL**，只有 `Cache Hit Ratio [com.hyao.mybatis.mapper.dict.DictItemMapper]: 0.5`（二级缓存命中） |
| `POST /dict/updateName`（改 `1` 的显示名）后再查 | `200`；再查时日志重新出现 `Preparing: SELECT ...`，`dictName` 是新值（`flushCache` 把缓存清掉了） |
| `POST /dict/updateName` 传不存在的 `dictCode` | `200` + `data:null`；日志 `<== Updates: 0`（接口没校验影响行数） |
| `POST /dict/list` `{"dictType":""}` | `400` + `{"code":"SYS0004","message":"参数校验失败: dictType字典类型不能为空","data":null}` |

> 从响应能看出几个知识点：① 详情里 `orderStatus` 是**数字**（枚举上的 `@JsonValue` 生效）；② 列表 `rows` 是**原始 map**，列名下划线、`status` 数字；③ 业务失败（库存不足、下架、状态不对）都是 `200 + 业务码`，参数校验失败才是 `400 + SYS0004`；④ 报表的 `statKey` 在 `status` 维度是数字字符串、在 `day` 维度是日期字符串——两套 SQL 的列别名保持同名才能共用同一个 VO；⑤ 判断二级缓存是否生效要看**日志**（第二次请求没有 `Preparing`、`Cache Hit Ratio` 上升），响应内容两次是一样的。

## 6. 常见问题与踩坑

| 现象 | 原因 / 解法 |
| --- | --- |
| `{"code":"SYS0004","message":"参数校验失败: pageSize分页大小不能为空"}` | `pageSize` 必填（`@NotNull`），列表接口必须传 |
| 明细里 `quantity:0`、`skuId:""` 没被拦下 | `OrderCreateRequest.items` 上没有 `@Valid`，嵌套约束不会级联；补上 `@Valid` 即可 |
| `{"code":"SKU0001","message":"商品不存在: [...]"}` | `sku_info` 里没有该 SKU；检查入参与初始化数据 |
| `{"code":"SKU0002","message":"商品已下架: SKU004"}` | `sku_info.status = 0`；下单前必须校验上架状态 |
| `{"code":"SKU0003","message":"库存不足: SKU001"}` | 扣库存影响行数 0；并发下不会扣成负数（条件是 `stock >= n`） |
| `{"code":"ORD0005",...}` / `{"code":"ORD0006",...}` | 状态不允许该操作（重复支付 / 重复取消 / 已发货再发货）；`WHERE status = 前置状态` 保证了幂等 |
| 创建订单后库存没变 / 变了没回滚 | 检查 `@Transactional` 是否在**对外方法**上（自调用会失效）；`createOrder` 里任何一步抛异常都会整体回滚 |
| 翻页重复或漏数据 | 游标语义要一致：本模块 `nextCursor` 是「多查一条」的 id，SQL 用 `id <= lastId`；换成 `id <` 就要把游标改成当前页最后一条 |
| `resultType="map"` 的 key 是 `user_id`、`status` 是数字 | 驼峰映射与 TypeHandler 只作用于实体；需要驼峰/枚举就返回实体，或用 `<resultMap>` |
| `Invalid bound statement (not found)` | `namespace` / statement `id` 与接口对不上，或 `mapper-locations` 没匹配到 XML |
| 枚举查出来报 `BIZ0001` | 库里值不在枚举范围（`OrderStatus` 0~4 / `SkuStatus` 0~1）；补枚举值或清洗数据 |
| 持久层异常返回 `SYS0001` 而不是 `DBE0001` | `DbExceptionHandler` 的 `@Order(1)` 标在方法上不生效；把 `@Order` 移到**类上** |
| 打开 `/druid` 报 `{"code":"SYS0003","message":"接口不存在: /druid"}` | YAML 缩进写错，`stat-view-servlet` 被嵌进 `filter` 下面（成了 `druid.filter.stat-view-servlet`），监控 Servlet 没注册。`stat-view-servlet`、`web-stat-filter`、`aop-patterns` 必须是 `spring.datasource.druid` 的直接子节点，与 `filter` **同级** |
| 集群部署后偶尔 `Duplicate entry` / 订单号重复 | 两个实例的 `workerId + datacenterId` 相同；给每个实例注入不同 `WORKER_ID` |
| 前端拿到的明细 id 精度丢失 | `order_item.id` 是 `Long`，JSON 数字超过 2^53 会丢精度；把 Long 序列化成字符串 |
| 启动报 `Communications link failure` / `Connection refused` | MySQL 没启动；或 url 的 `useSSL=true&requireSSL=true` 与服务器不一致 |
| SQL 日志没有 traceId / 没进文件 | 用了 `log-impl: StdOutImpl`；改用 `logging.level.<mapper包>: debug` |
| `Parameter 'xxx' not found` | 参数名与 `#{}` 对不上：单参数 POJO/Map 可直接用属性名，多参数必须 `@Param` |
| 字典连查两次，第二次日志里没有 SQL、命中率上升 | 正常现象：二级缓存命中（`Cache Hit Ratio [com.hyao.mybatis.mapper.dict.DictItemMapper]: 0.5`），只有第一次查询打 SQL |
| 用 SQL 客户端改了 `dict_item`，接口还返回旧值 | 二级缓存不会被外部更新清掉（只有**同 namespace** 的 update 才 `flushCache`）；等 `flushInterval`（10 分钟）过期或重启应用 |
| 多实例部署时各实例字典数据短暂不一致 | 二级缓存是本地缓存，每个实例一份；要跨实例一致就得换 Redis 版的 `Cache` 实现 |
| `POST /dict/updateName` 返回 `200` 但数据没变 | `dictCode` 不存在，影响 0 行；`DictService` 没校验影响行数（实测日志 `Updates: 0`） |
| `statistics` 传 `dimension:"week"` 却返回按状态的统计 | `<choose>` 只认 `day`，其它值都走 `<otherwise>` 静默兜底；拦下来的是 DTO 上的 `@Pattern(regexp="day|status")`（实测 `SYS0004`） |
| 按天统计少了最后一天的数据 | 上界写成 `<= #{endDate}` 时，`LocalDate` 绑参是当天 `00:00:00`；要用 `< DATE_ADD(#{endDate}, INTERVAL 1 DAY)` |
| `statistics` 报 `500` / `SYS0001` | 库里 `status` 超出 0~4，`OrderStatus.ofCode` 返回 `null`，随后 `.getDesc()` NPE |
| 按天统计的 `statusDesc` 是 `null` | `day` 分支不补状态描述，只有 `dimension=status` 才会查枚举填进去 |

## 7. 学习笔记

- **注解 Mapper vs XML Mapper 怎么分工**：注解适合「单表 + 一句话 SQL」（`SkuStockMapper`）；只要出现动态 SQL、`resultMap`、批量，就放 XML——注解里写 `<script>` 虽然能跑，但可读性差。
- **Mapper 接口为什么不用实现类**：`@MapperScan` → `MapperFactoryBean` → `MapperProxy`（JDK 动态代理），调用最终落到 `SqlSessionTemplate`。
- **分布式主键（雪花）的取舍**：不用数据库自增，换来水平扩展能力；代价是必须给每个实例分配唯一 `workerId`，并处理时钟回拨（回拨 ≤5ms 等待，超过拒绝生成）。
- **库存扣减为什么用条件更新**：`UPDATE ... SET stock = stock - ? WHERE stock >= ?` 把「判断 + 扣减」放在一条 SQL 里，靠影响行数判断结果，不依赖先读后写，并发下不会超卖；乐观锁 `version` 适合「读多改少且需要检测并发修改」的场景。
- **状态机写在 `WHERE` 里**：`WHERE id = ? AND status = 前置状态`，影响行数 1 才算成功——重复请求第二次影响 0 行，天然幂等。
- **keyset 游标分页**：`WHERE id <= 游标 ORDER BY id DESC LIMIT n+1`，多查一条判断 `hasNextPage`，把多出来的那条作为 `nextCursor`；深翻页成本恒定，但只能顺序翻、并且排序键必须唯一且可比较。
- **`resultType="map"` 的边界**：列名不驼峰、枚举不转换——列表页要「原样返回」时很方便，需要类型安全就返回实体/VO。
- **事务与连接的关系**：`@Transactional` 下「扣库存 + 插订单 + 批插明细」共用一条连接，任何一步异常整体回滚；这也是一级缓存只在同一 SqlSession/事务内有效的原因。
- **`<if>` 还是 `<choose>`**：同一套 SQL 加减条件用 `<if>`；两套互斥的 SQL（查询列、`GROUP BY` 都不同）用 `<choose>/<when>/<otherwise>`，而且分支参数必须自己校验，`<otherwise>` 会静默兜底。
- **聚合查询的 `map` 契约**：`resultType="map"` 的 key 是**列别名**，别名一变 Service 就取不到值；`COUNT` 返回 `Long`、`SUM` 在无数据时是 `null`，都要自己兜底。
- **二级缓存的三个边界**：本地（不跨实例）、namespace 隔离（别的 mapper 写库不会清它）、`readOnly` 共享实例（别改返回的对象）。一级缓存挂在 `SqlSession` 上，所以「同一事务里查两次」才有意义，跨请求靠的是二级缓存。
- **待补充**：多数据源（`order`/`product`/`dict` 分包已完成，等接第二个库）。