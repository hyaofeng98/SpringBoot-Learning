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

在 Spring Boot 中集成 MyBatis 与 Druid：注解 / XML 两种 Mapper 写法、动态 SQL、驼峰映射与枚举 TypeHandler、keyset 分页、声明式事务、持久层异常与业务错误码，并理清一次 Mapper 调用从代理到 Druid 取连接的链路。

## 2. 学习清单

按**能力域**聚合，一个域一行：

| 状态 | 优先级 | 知识点 | 说明 |
| --- | --- | --- | --- |
| ✅ | — | **集成与配置** | `mybatis-spring-boot-starter` + `druid-spring-boot-starter` + MySQL 驱动；`@MapperScan` 统一扫描；常用配置清单（数据源 / Druid 连接池 / `mybatis.*` / `mybatis.configuration.*`，见 §3.2） |
| ✅ | — | **注解式 Mapper（`UserProfilesMapper`）** | `@Insert` / `@Select` / `@Update` + `@Param` + `#{}`；演示「全字段更新会覆盖未传字段」的坑（见 §4.2.1） |
| ✅ | — | **XML 式 Mapper（`UserMapper` + `user.mapper.xml`）** | `namespace` 绑定、`<sql>` + `<include>`、`resultType` 的实体 / map 两种、`LIMIT` 参数化；statement `id` 必须与接口方法名一致（见 §4.2.3） |
| ✅ | — | **动态 SQL（`<where>` / `<set>` / `<if>`）** | `<where>` 自动去掉首个 `AND`、`<set>` 自动去掉尾逗号，配合 `<if>` 实现条件查询与选择性更新 |
| ✅ | — | **Keyset（游标）分页** | `WHERE u.id >= #{nextCursor} ORDER BY u.id LIMIT #{pageSize}`，多查一条判断 `hasNextPage` 并返回下一页 `nextCursor` |
| ✅ | — | **声明式事务 `@Transactional`** | `insertUser` 上 `@Transactional(rollbackFor = Exception.class)`：`user` 与 `user_profiles` 两条 insert 同一事务、一起提交或回滚 |
| ✅ | — | **枚举 ↔ 整型字段（TypeHandler）** | `BaseEnum` 约定 `getCode()` + 泛型基类 `BaseEnumTypeHandler<E>` + `GenderTypeHandler` / `UserStatusTypeHandler` + `type-handlers-package` 注册 |
| ✅ | — | **持久层异常** | 模块自己的 `DbExceptionHandler`：`DataAccessException` → `DBE0001`，靠 `@Order` 优先于公共库的兜底方法（见 §4.2.4） |
| ✅ | — | **SQL 日志** | 走 slf4j（`logging.level.com.hyao.mybatis.mapper: debug`），进入 00 模块的统一日志格式并带 traceId |
| ⬜ | ★★★ | **主键生成策略** | 示例里 `insertUser` 故意把 id 写死成 `u0000001`（同一 id 第二次插入会主键冲突）；真实项目可换 UUID / 雪花 / 号段 |
| ⬜ | ★★★ | **`resultMap` 与关联查询** | 现在全靠 `resultType` + 驼峰；可补 `<resultMap>` / `<association>`（`user` 与 `user_profiles` 正好一对一）/ `<collection>` / 延迟加载 |
| ⬜ | ★★☆ | **动态 SQL 补全** | 缺 `<foreach>`（in 查询 / 批量插入）、`<choose>`、`<trim>`；条件判空的写法已在 `pageQueryUserConditional` 里用 `<if test="nextCursor != null and nextCursor != ''">` 演示 |
| ⬜ | ★★☆ | **批量操作** | `<foreach>` 批量 insert、`ExecutorType.BATCH` 批量模式 |
| ⬜ | ★★☆ | **分页插件** | 手写 keyset 分页够用，可对比 PageHelper；顺带讲 MyBatis 插件 `Interceptor` + `@Intercepts` + `@Signature` |
| ⬜ | ★★☆ | **测试** | `@MybatisTest` 切片测试、`@SpringBootTest` + `@Transactional` 回滚、H2 内存库 |
| ⬜ | ★☆☆ | **缓存（一级 / 二级）** | 一级缓存（SqlSession 级、同一事务内）与二级缓存（namespace 级、`<cache/>` + `cache-enabled`） |
| ⬜ | ★☆☆ | **`${}` 与 SQL 注入** | 现在全用 `#{}`；动态排序 / 表名不得不用 `${}` 时怎么防注入 |
| ⬜ | ★☆☆ | **乐观锁** | 并发更新丢失问题（`version` 字段或 `WHERE` 带上旧值） |
| ⬜ | ★☆☆ | **工程化** | 建表脚本自动化（`spring.sql.init.*` 或 Flyway）、多环境与密码外置、Druid 监控页（`stat-view-servlet` + 慢 SQL 统计） |

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
| `mapper-locations`                | XML mapper 位置（`classpath*:mapper/**/*.xml`），**不配 XML 不会被加载** |
| `type-aliases-package`            | 扫描该包并注册短类名别名（`resultType="User"`）                       |
| `type-handlers-package`           | 扫描该包并注册自定义 TypeHandler                                     |
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
| `map-underscore-to-camel-case`          | 下划线列名 → 驼峰属性（`user_name` → `userName`），**只对 POJO 生效** |
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

#### 3.2.5 日志与建表（Spring 侧）

| 配置                                                                     | 说明                                                     |
| ------------------------------------------------------------------------ | -------------------------------------------------------- |
| `logging.level.<mapper 包>`                                               | SQL 日志级别（`debug` 打印 SQL、`trace` 还打印结果集），MyBatis 按 **Mapper 方法**打日志 |
| `spring.sql.init.mode` / `spring.sql.init.schema-locations` / `spring.sql.init.data-locations` | 启动时执行建表 / 初始化数据脚本         |

### 3.3 建表脚本

[`script/1.sql`](src/main/resources/script/1.sql) 定义两张表，**需要自己在 MySQL 里先执行**：

| 表                  | 说明                                                                                                                                                       |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `user`          | 用户表：`id`(varchar(16) 业务主键)、`nickname`、`avatar`、`status`(tinyint)、`created_at`、`updated_at`、`deleted_at`                                      |
| `user_profiles` | 扩展资料表：`user_id`(主键)、`real_name`、`gender`(tinyint)、`birthday`(date)、`province`、`city`、`address`、`company`、`bio`、`created_at`、`updated_at` |

> 脚本里的表名与代码（XML / 注解 SQL）一致；执行前确认库名与 `spring.datasource.url` 里的一致。

## 4. 核心实现

### 4.1 启动配置

**① 启动类上的 `@MapperScan`（启动 Mapper 扫描的必要注解）**

```java
@SpringBootApplication
@MapperScan("com.hyao.mybatis.mapper")   // 统一扫描，Mapper 接口不用加 @Mapper
public class MybatisApplication { ... }
```

- 作用：把指定包下所有接口注册成 Mapper Bean，之后才能 `@Autowired UserMapper`；
- 机制：`MapperScannerConfigurer` 为每个接口注册 `MapperFactoryBean`，`getObject()` 返回 JDK 动态代理（`MapperProxy`）——**所以 Mapper 接口不需要实现类**；
- 替代方式：逐个接口加 `@Mapper`，由 MyBatis 自动配置里的 `AutoConfiguredMapperScannerRegistrar` 扫描；它带 `@ConditionalOnMissingBean(MapperScannerConfigurer.class)`，所以两种方式**二选一**；

**② 配置项三件套（不配不报错，但功能不生效）**

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml         # XML mapper 才会被加载
  type-aliases-package: com.hyao.mybatis.entity    # resultType 可用短类名
  type-handlers-package: com.hyao.mybatis.handler  # 枚举 TypeHandler 才会被注册
```

- 漏 `mapper-locations` → XML 不加载，调用报 `Invalid bound statement`；漏 `type-aliases-package` → 短类名找不到；漏 `type-handlers-package` → 枚举字段转不了。
- 完整的 `mybatis.*` / `mybatis.configuration.*` 配置项见 §3.2.3、§3.2.4。

### 4.2 具体实现

#### 4.2.1 注解式 Mapper（`UserProfilesMapper`）

SQL 直接写在接口方法的注解里：

```java
public interface UserProfilesMapper {
    @Insert("INSERT INTO user_profiles(user_id, real_name, birthday, province, city, address, company, bio) " +
            "VALUES (#{userId}, #{realName}, #{birthday}, #{province}, #{city}, #{address}, #{company}, #{bio})")
    int insertUserProfiles(UserProfiles userProfiles);

    @Select("SELECT * FROM user_profiles WHERE user_id=#{userId}")
    UserProfiles queryUserProfileByUserId(@Param("userId") String userId);

    @Update("UPDATE user_profiles SET bio = #{bio} WHERE user_id = #{userId}")
    int updateUserBio(@Param("userId") String userId, @Param("bio") String bio);

    @Update("UPDATE user_profiles SET real_name=#{realName}, birthday=#{birthday}, province=#{province}, " +
            "city=#{city}, address=#{address}, company=#{company}, bio=#{bio} WHERE user_id=#{userId}")
    int updateUserProfiles(UserProfiles userProfiles);
}
```

要点：

- **`@Param`**：多参数（`updateUserBio`）必须写，否则只能用 `#{arg0}`/`#{param1}`；
- **`#{}` 是预编译占位符**（`PreparedStatement` 的 `?`），天然防注入；
- **全字段 `UPDATE` 的坑**（代码注释里也写了）：`updateUserProfiles` 会把所有列都写回去，如果只 set 了部分字段，**没 set 的字段会被写成 `NULL`**——所以要么先查出完整对象再更新，要么像 `updateUserBio` 那样只更新需要改的列；
- **`gender` 也走 TypeHandler**：`insertUserProfiles` / `updateUserProfiles` 都包含 `gender` 列，`UserProfiles.gender` 是 `Gender` 枚举，写入时由 `GenderTypeHandler` 转成 `tinyint`。

#### 4.2.2 枚举 ↔ 整型字段：自定义 TypeHandler

三步：

1. **枚举约定一个接口**：

   ```java
   public interface BaseEnum { int getCode(); }                 // 数据库里存的整型
   public enum UserStatus implements BaseEnum { DISABLED(0,"禁用"), NORMAL(1,"正常"), DELETED(2,"删除"); ... }
   public enum Gender     implements BaseEnum { UNKNOWN(0,"未知"), MALE(1,"男"), FEMALE(2,"女"); ... }
   ```

2. **写一个泛型基类**（[`BaseEnumTypeHandler<E extends Enum<E> & BaseEnum>`](src/main/java/com/hyao/mybatis/handler/BaseEnumTypeHandler.java)）：构造时把 `code → 枚举` 放进 Map；`setNonNullParameter` 写 `getCode()`；三个 `getNullableResult` 读整型再反查（`rs.wasNull()` 返回 `null`，未知 code 直接抛异常而不是静默返回 null）。

3. **子类只需空壳 + 注解**：

   ```java
   @MappedTypes(Gender.class)
   @MappedJdbcTypes(JdbcType.INTEGER)
   public class GenderTypeHandler extends BaseEnumTypeHandler<Gender> {
       public GenderTypeHandler() { super(Gender.class); }
   }
   ```

   再由 `mybatis.type-handlers-package: com.hyao.mybatis.handler` 统一注册生效。

> `UserStatus` / `Gender` 提供了静态工厂 `of(int code)`，`BaseEnumTypeHandler.codeOf()` 也对未知 code 抛 `BizException(BizErrorCode.BIZ_ENUM_ERROR)`（`BIZ0001`）——两处都收敛到同一个业务错误码上，脏数据不会静默变成 `null`。

#### 4.2.3 XML 式 Mapper（`UserMapper` + `user.mapper.xml`）

接口方法上**没有任何 SQL 注解**，SQL 全在 XML 里：

```java
public interface UserMapper {
    User queryUserById(String id);                                            // 演示 <sql> + <include>
    List<Map<String, Object>> pageQueryUserConditional(Map<String, Object> params);  // 演示 <where> + <if>
    int insertUser(User user);
    int updateUserSelective(User user);                                       // 演示 <set> + <if>
}
```

```xml
<mapper namespace="com.hyao.mybatis.mapper.UserMapper">

    <sql id="Base_Column_List">
        id, nickname, avatar, status, created_at, updated_at
    </sql>

    <select id="queryUserById" parameterType="string" resultType="User">
        SELECT
            <include refid="Base_Column_List" />
        FROM user
        WHERE id = #{id}
    </select>

    <select id="pageQueryUserConditional" parameterType="map" resultType="map">
        SELECT u.id, u.nickname, u.avatar, u.status, u.created_at, u.updated_at,
               up.gender, up.birthday, up.province, up.city, up.address, up.company, up.bio
        FROM `user` u
        LEFT JOIN user_profiles up ON u.id = up.user_id
        <where>
            <if test="nextCursor != null and nextCursor != ''">u.id &gt;= #{nextCursor}</if>
            <if test="status != null">AND u.status = #{status}</if>
            <if test="gender != null">AND up.gender = #{gender}</if>
            <if test="beginDate != null">AND up.birthday &gt;= #{beginDate}</if>
            <if test="endDate != null">AND up.birthday &lt;= #{endDate}</if>
            <if test="province != null">AND up.province = #{province}</if>
            <if test="city != null">AND up.city = #{city}</if>
        </where>
        ORDER BY u.id ASC
        LIMIT #{pageSize}
    </select>

    <update id="updateUserSelective" parameterType="User">
        UPDATE `user`
        <set>
            <if test="nickname != null">nickname = #{nickname},</if>
            <if test="avatar != null">avatar = #{avatar},</if>
            <if test="status != null">status = #{status},</if>
            <if test="deletedAt != null">deleted_at = #{deletedAt},</if>
        </set>
        WHERE id = #{id}
        AND status != 2
    </update>
</mapper>
```

**XML 里用到的知识点**

| 点 | 说明 |
| --- | --- |
| `namespace` | 必须是 Mapper 接口的全限定名，MyBatis 靠它把接口和 XML 绑定起来 |
| statement `id` | 必须等于接口方法名，否则 `Invalid bound statement (not found)` |
| `<sql>` + `<include>` | 把列清单抽成片段复用，避免到处写 `SELECT *` |
| `<where>` | 自动去掉开头多余的 `AND` / `OR`，所以每个 `<if>` 里可以放心写 `AND ...` |
| `<set>` | 自动去掉末尾多余的逗号，配合 `<if>` 实现"只更新非空字段" |
| `parameterType` | 可省略；`string` / `map` / `User` 用的是别名（`type-aliases-package`） |
| `resultType` | 实体写 `User`（走驼峰映射 + TypeHandler）；联表/多列直接返回 `map` |
| XML 转义 | `>=`、`<=` 要写成 `&gt;=`、`&lt;=`，或整段包进 `<![CDATA[ ]]>` |
| `LIMIT #{pageSize}` | 占位符可以用于 `LIMIT`（MySQL 支持），不要用 `${}` 拼 |

**分页为什么是 `id >= #{nextCursor}`**：这是 keyset（游标）分页——不做 `offset` 跳页，翻页时带上「上一页返回的下一页起始 id」；第一页没有游标，`<if test="nextCursor != null and nextCursor != ''">` 会把整个条件省掉，`ORDER BY id ASC LIMIT n` 因此**深翻页不会越来越慢**。代价是只能顺序翻页，而且排序键必须唯一且可比较（这里 `id` 是 `u0000001` 这种可比较的字符串）。`UserService` 里用「多查一条」判断 `hasNextPage`，并把多出来那条的 id 作为 `nextCursor` 返回。

**实测（本地 MySQL，2026-10-18 运行结果）**

| 请求 | 实际响应 |
| --- | --- |
| `POST /user/queryById`，body `{"id":"u0000001"}` | `{"code":"SUC0000","message":"操作成功","data":{"nickname":"hyao feng","id":"u0000001","avatar":"123","createAt":"2026-10-18","status":"NORMAL"}}` |
| `POST /user/pageQueryConditional`，body `{"pageSize":2}` | `{"code":"SUC0000","message":"操作成功","data":{"hasNextPage":true,"nextCursor":"u0334220","resultData":[{"id":"u0000001","created_at":"2026-10-18T15:29:41","updated_at":"2026-10-18T16:16:02","gender":0,...}, ...]}}` |
| `POST /user/pageQueryConditional`，body `{}` | `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize: 不能为空","data":null}`（参数校验迁到 DTO + `@Valid` 后，见 §4.2.5） |

> 从响应里能看出两个知识点：① 实体查询时 `status` 返回的是**枚举名** `"NORMAL"`（库里 `status=1`，与 `UserStatus.NORMAL(1)` 一致）；② `resultType="map"` 时列名保持**下划线**（`created_at`）、`gender` 是**原始数字**、时间按 ISO 输出——**驼峰映射和 TypeHandler 都不作用于 Map**。

#### 4.2.4 持久层异常：模块自己的 advice

[`DbExceptionHandler`](src/main/java/com/hyao/mybatis/exception/DbExceptionHandler.java)：

```java
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)          // 关键：必须优先于公共库的 Exception 兜底
@RestControllerAdvice
public class DbExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库操作异常", e);        // 细节（SQL、表名）只进日志
        return Result.fail(BizErrorCode.DB_ERROR.getCode(), BizErrorCode.DB_ERROR.getMessage());
    }
}
```

**为什么必须加 `@Order(HIGHEST_PRECEDENCE)`**：00 模块的全局异常处理里有一个 `@ExceptionHandler(Exception.class)` 兜底方法，它同样能匹配 `DataAccessException`；多个 advice 之间是「按 `@Order` 排序后取第一个能匹配的方法」，不排到最前面的话，持久层异常会先被兜底接走变成 `SYS0001`。实测（临时探针，已删除）：

| 抛出的异常                                                         | 处理者               | 响应                                                      |
| ------------------------------------------------------------------ | -------------------- | --------------------------------------------------------- |
| `DataAccessResourceFailureException`（`DataAccessException` 子类） | `DbExceptionHandler` | `200` + `{"code":"DBE0001",...}`                          |
| `IllegalStateException`                                            | 00 模块兜底方法      | `200` + `{"code":"SYS0001","message":"系统内部异常",...}` |

因此 Service 里不用再写 try-catch：持久层异常自动带 `DBE0001`，其他异常走公共库兜底；SQL、表名等细节只进日志。

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
userMapper.queryUserById("u0000001")              // MapperProxy（JDK 动态代理）
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
- **有没有 `@Transactional` 行为不同**：没有事务时"借 → 用 → 还"（下一条 SQL 再借）；有事务时连接绑在线程上，**同一事务共用一条连接**，直到提交/回滚才释放——`UserService.insertUser` 上就有一个 `@Transactional`，两条 insert 共用同一条连接；
- **换连接池为什么只改配置**：MyBatis 只依赖 `javax.sql.DataSource` 接口，`SpringManagedTransaction` 拿到的就是容器里的 DataSource Bean。

**想自己验证**：在 `SpringManagedTransaction.getConnection()`、`DataSourceUtils.getConnection()`、`DruidDataSource.getConnectionInternal()` 上打断点看调用栈；或用 `DruidDataSource.getActiveCount()`（开 Druid 监控页）对比有无事务；MySQL 侧 `SHOW PROCESSLIST` 看物理连接数应在 `initial-size`/`min-idle` 量级。

### 4.4 容易踩坑的点

- `@MapperScan` 的包必须覆盖**所有** Mapper 接口（本模块踩过：包名写旧 → 启动后 Mapper 注入失败）；Mapper 接口**不要**同时写实现类，否则调用会绕过代理走你的实现；
- `@MapperScan` 与 `@Mapper` 二选一：配了 `@MapperScan` 后 MyBatis 的自动 `@Mapper` 扫描器会退让（`@ConditionalOnMissingBean(MapperScannerConfigurer.class)`），**另一个包里的 `@Mapper` 接口不会被扫到**；
- Spring Boot 2.6+ 默认 `spring.main.allow-bean-definition-overriding=false`，同一 Mapper 接口被扫描注册两次会直接启动失败；
- 自定义 TypeHandler 必须被注册（`type-handlers-package` 或 `@MappedTypes`），否则枚举字段转换失败；
- XML 的 `namespace` 是接口全限定名、statement `id` 必须等于方法名，否则 `Invalid bound statement`；
- **`resultType="map"` 不走驼峰、不走 TypeHandler**：列名保持下划线、枚举返回数字（实测见 §4.2.3）；
- `<where>` 会自动去掉首个 `AND`/`OR`、`<set>` 会自动去掉尾逗号——所以 `<if>` 里可以放心写 `AND ...`、`xxx = #{xxx},`；但 `<set>` 里**一个字段都没有**时会生成非法的 `UPDATE ... SET WHERE`；
- XML 里 `>`、`<` 要写成 `&gt;`、`&lt;`（或包进 CDATA），否则 XML 解析失败；
- keyset 分页要求排序键**唯一且可比较**（`id` 是字符串时按字典序），否则会漏数据或重复；
- `useGeneratedKeys="true" keyProperty="id"` 只能用于**自增主键**表（本模块 id 是 varchar 手动生成的，所以 XML 的 insert 没写它）；
- `mybatis.configuration.log-impl: StdOutImpl` 会绕过 slf4j：SQL 日志不带 traceId、不进文件；改用 `logging.level.<mapper包>: debug`；
- `config-location` 与 `configuration.*` 同时配置会启动报错，二选一；
- Druid：`max-wait` 打满后抛 `GetConnectionTimeoutException`；`test-while-idle` 要配 `validation-query` 才有意义；`initial-size` 在库连不上时启动即失败；
- 枚举未知 `code` 会抛异常而不是静默返回 `null`（枚举工厂和 `BaseEnumTypeHandler` 都抛 `BIZ0001`）——这是有意的，让脏数据早暴露。

> 数据 / 表名 / 字段层面的检查结果（已修复项 + 仍存在的 4 条）集中记录在 §4.2.3 的问题清单。

## 5. 快速验证

前置条件：

1. 启动本地 MySQL，创建库 `hyao`；
2. 执行 [`script/1.sql`](src/main/resources/script/1.sql)（建 `user`、`user_profiles`，表名与代码一致）；
3. 按需修改 `application.yaml` 的账号密码（`useSSL=true&requireSSL=true` 要求 MySQL 开了 SSL，本地没开会连不上，去掉 `requireSSL` 即可）；
4. IDEA 里运行 [`MybatisApplication`](src/main/java/com/hyao/mybatis/MybatisApplication.java) 的 `main`（端口 8083，同一端口同时只能跑一个实例）。

```bash
# 按 id 查询（XML Mapper：<sql> + <include>）
curl -X POST http://localhost:8083/user/queryById \
  -H 'Content-Type: application/json' -d '{"id":"u0000001"}'

# 条件分页（XML Mapper：<where> + <if> + keyset 分页）
curl -X POST http://localhost:8083/user/pageQueryConditional \
  -H 'Content-Type: application/json' -d '{"pageSize":2}'
curl -X POST http://localhost:8083/user/pageQueryConditional \
  -H 'Content-Type: application/json' -d '{"pageSize":3,"status":1}'

# 参数校验失败的情况（缺 pageSize / 超出范围 / 日期格式错）
curl -X POST http://localhost:8083/user/pageQueryConditional \
  -H 'Content-Type: application/json' -d '{}'
curl -X POST http://localhost:8083/user/pageQueryConditional \
  -H 'Content-Type: application/json' -d '{"pageSize":1000}'
curl -X POST http://localhost:8083/user/pageQueryConditional \
  -H 'Content-Type: application/json' -d '{"pageSize":2,"beginDate":"2020/01/01"}'

# 写操作（会改数据，按需执行）
curl -X POST http://localhost:8083/user/insert -H 'Content-Type: application/json' \
  -d '{"nickname":"hyao","avatar":"","birthday":"2000-01-01","gender":1}'
curl -X POST http://localhost:8083/user/update -H 'Content-Type: application/json' \
  -d '{"id":"u0000002","nickname":"hyao feng","avatar":"123"}'
curl -X POST http://localhost:8083/user/delete -H 'Content-Type: application/json' \
  -d '{"id":"u0000002"}'
```

实测（只跑了查询，写接口没跑以免改动现有数据）：

| 请求 | 结果 |
| --- | --- |
| `POST /user/queryById` `{"id":"u0000001"}` | `200` + `{"code":"SUC0000","message":"操作成功","data":{"nickname":"hyao feng","id":"u0000001","avatar":"123","createAt":"2026-10-18","status":"NORMAL"}}` |
| `POST /user/pageQueryConditional` `{"pageSize":2}` | `200` + `data.hasNextPage=true`、`data.nextCursor="u0334220"`、`data.resultData` 2 条（key 为 `created_at` 等下划线列名） |
| `POST /user/pageQueryConditional` `{"pageSize":2,"nextCursor":"u0272f98"}` | `200`，第二页从 `u0272f98` 开始（包含式游标），`data.nextCursor="u048f425"` |
| `POST /user/pageQueryConditional` `{}` | `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize: 不能为空","data":null}` |
| `POST /user/pageQueryConditional` `{"pageSize":0}` | `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize: 至少为 1","data":null}` |
| `POST /user/pageQueryConditional` `{"pageSize":1000}` | `400` + `{"code":"SYS0004","message":"参数校验失败: pageSize: 最多为 100","data":null}` |
| `POST /user/pageQueryConditional` `{"pageSize":"abc"}` | `400` + `{"code":"SYS0004","message":"参数校验失败，请求体解析失败","data":null}` |
| `POST /user/pageQueryConditional` `{"pageSize":2,"beginDate":"2020/01/01"}` | `400` + `{"code":"SYS0004","message":"参数校验失败: beginDate: 格式必须为 yyyy-MM-dd","data":null}` |
| `POST /user/pageQueryConditional` `{"pageSize":"2"}` | `200`（Jackson 把字符串 `"2"` 强转成数字），要触发解析失败得传非数字 |

SQL 日志（走统一日志格式，带 traceId；下面截取了实测日志的可见部分）：

```
DEBUG [faf9c972e15e43e1ad4bc7141bd8ab8d] c.h.m.mapper.UserMapper.queryUserById - ==>  Preparing: SELECT id, nickname, avatar, status, created_at, updated…
DEBUG [faf9c972e15e43e1ad4bc7141bd8ab8d] c.h.m.mapper.UserMapper.queryUserById - <==      Total: 1
DEBUG [d78ccffd9a3e44c0bf8893e51d9bf06d] c.h.m.m.U.pageQueryUserConditional - ==>  Preparing: SELECT u.id, u.nickname, u.avatar, u.status, u.created_at, …
DEBUG [d78ccffd9a3e44c0bf8893e51d9bf06d] c.h.m.m.U.pageQueryUserConditional - <==      Total: 3
```

## 6. 常见问题与踩坑

| 现象 | 原因 / 解法 |
| --- | --- |
| `Table 'hyao.user' doesn't exist` | 代码里表名是 `user` / `user_profiles`（脚本已同步）；如果你用的是老脚本（`user_tbl` / `user_profiles_tbl`），先统一表名 |
| `resultType="map"` 返回的 key 是 `created_at`、`gender` 是数字 | 驼峰映射与 TypeHandler 都只作用于 POJO / 实体；需要驼峰或枚举就返回实体，或用 `<resultMap>` 显式映射 |
| `UserProfiles` 的 `createdAt` / `updatedAt` 拿到 `null` | 列名是 `created_at` / `updated_at`，驼峰映射对应 `createdAt` / `updatedAt`；字段名与列名不一致时要么改名，要么写 `<resultMap>` |
| `Duplicate entry 'u0000001' for key 'PRIMARY'` | `insertUser` 里 id 写死成 `u0000001`，第二次插入就冲突（需要 ID 生成策略） |
| `SQLSyntaxErrorException ... 'where clause'` | `updateUserSelective` 的 `<if>` 都不命中，生成了 `UPDATE user SET WHERE id = ?`；在 `<set>` 末尾加 `updated_at = updated_at` 兜底，或业务层提前拒绝空更新 |
| `deleted_at` 一直是 `1970-01-01` | 逻辑删除只改了 `status=2`，没有写 `deleted_at` |
| `{"code":"SYS0004","message":"参数校验失败: pageSize: 不能为空"}` | `pageQueryConditional` 缺 `pageSize`（`@Valid` 生效，返回 400） |
| `{"code":"SYS0004","message":"参数校验失败，请求体解析失败"}` | 请求体不是合法 JSON，或字段类型无法转换（如 `pageSize:"abc"`）；注意 `"2"` 这种字符串会被 Jackson 强转成数字，不会报错 |
| 参数校验根本没触发 | 忘了在参数上标 `@Valid`；或者想校验 `@RequestParam` / `@PathVariable` 却没在 Controller 类上标 `@Validated` |
| `Invalid bound statement (not found)` | `namespace` / statement `id` 与接口对不上，或 `mapper-locations` 没匹配到 XML 文件 |
| `Unknown code: 9` / `BIZ0001` | 库里值不在枚举范围内：枚举工厂与 `BaseEnumTypeHandler` 都抛 `BizException(BIZ_ENUM_ERROR)`；要么补枚举值，要么清洗数据 |
| 启动报 `Communications link failure` / `Connection refused` | MySQL 没启动；或 url 里的 `useSSL=true&requireSSL=true` 与服务器不一致（本地没开 SSL 就去掉 `requireSSL`） |
| SQL 日志没有 traceId / 没进文件 | 用了 `log-impl: StdOutImpl`；删掉它、改用 `logging.level.<mapper包>: debug` |
| `MessageFormat` 的参数被吞掉 | `new BizException(code, "参数")` 的参数是填进 `code.getMessage()` 占位符的；文案里没有 `{0}` 时会被静默忽略（`DB_ERROR` 就没有占位符） |
| `Parameter 'xxx' not found` | 多参数没用 `@Param`（单参数是 POJO / Map 时才可省略） |
| XML mapper 根本没被加载 | `mapper-locations` 与文件位置不匹配（`*` 只匹配一层目录） |

## 7. 学习笔记

- **Mapper 接口为什么不用实现类**：`@MapperScan` → `MapperFactoryBean` → `MapperProxy`（JDK 动态代理），调用最终落到 `SqlSessionTemplate`。
- **枚举落库的三种做法**：`EnumTypeHandler`（存 `name()` 字符串）、`EnumOrdinalTypeHandler`（存 `ordinal()` 序号）、自定义 TypeHandler（存自己的 `code`）。本模块选第三种：**序号会随枚举顺序变化、字符串占空间**，用自定义 `code` 最稳。
- **全字段更新 vs 选择性更新**：`UPDATE ... SET 所有列` 必须先把整行查出来再改，否则没传的字段会被覆盖成 `NULL`；`<set>` + `<if>` 才是"只更新传了的字段"的正确姿势。
- **keyset（游标）分页 vs offset 分页**：`WHERE id >= 游标 ORDER BY id LIMIT n` 的深翻页成本恒定；`LIMIT offset, n` 的 `offset` 越大越慢。keyset 的代价是不能跳页，且游标键必须唯一、可比较。
- **公共库与本模块的分工**：响应包装 / 全局异常兜底 / **参数校验** / traceId 在 `00-springboot-common`；本模块只补「持久层异常 → `DBE0001`」、业务错误码（`BIZ0001`），以及校验用的 DTO 和约束。
- **空 `<set>` 不能靠 `<set>` 自己兜住**：`<set>` 只负责去掉多余逗号，一个字段都没有时仍会生成非法的 `SET WHERE`；要么业务层拒绝空更新，要么在末尾放一句恒真赋值（`updated_at = updated_at`）。
- **keyset 分页的游标语义**：本模块用的是**包含式**游标（`id >= nextCursor`，`nextCursor` = 下一页第一条记录的 id，所以第一页没有游标）；也可以改成**排除式**（`id > lastId`，`lastId` = 上一页最后一条）、更常见、不易混淆。两种都要求排序键唯一且可比较，并按 `(排序键)` 建索引。
- **练习路线**：先把 §4.2.3 里"还剩一点"收尾（空 `<set>` 兜底），再按 §2 里 ⬜ 的优先级补（主键生成策略 → `resultMap`/关联查询 → 动态 SQL 补全 → 批量 → 测试 → 缓存），每补完一项就把状态改成 ✅。
