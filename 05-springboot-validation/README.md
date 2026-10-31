# 05-springboot-validation：Spring Boot 参数校验

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**（随代码变更同步更新），内容按实际代码与实跑结果核对过，但仍可能有疏漏或滞后，**如与代码不一致请以源码为准**。

## 这个模块做什么

这个模块用一组最小示例，把 Spring Boot 里 **参数校验（Bean Validation）** 的常见用法按 11 个场景串起来：从「一个注解搞定字段非空」，到「分组、级联分组转换、动态分组、分组顺序」，再到「自定义注解 / 复合注解 / 需要依赖 Spring Bean 的校验器」，最后是「不走注解的编程式校验」。

<ul>
<li><b>标准 + 实现</b>：规则来自 <b>Jakarta Bean Validation 3.0</b>（<code>jakarta.validation</code> 包），运行时实现是 Boot 自带的 <b>Hibernate Validator 8.0.3.Final</b>；<code>org.hibernate.validator.constraints.*</code> 是它的扩展注解，<code>org.hibernate.validator.group.*</code> 是动态分组的扩展点。</li>
<li><b>声明式为主</b>：绝大多数场景只需「DTO 字段上加注解」+「接口参数上加 <code>@Valid</code> / <code>@Validated(groups)</code>」，校验失败由 <a href="../00-springboot-common/README.md">00 模块</a>的全局异常处理统一转成 <code>400 + SYS0004</code>，业务代码里没有一行 <code>if (x == null)</code>。</li>
<li><b>零配置</b>：<code>application.yaml</code> 只有 <code>server.port: 8085</code>，校验能力完全由依赖 + 注解驱动，没有手写 <code>Validator</code> Bean、没有 <code>@EnableMethodValidation</code> 之类的开关。</li>
</ul>

<p>11 个示例（与 <code>controller</code> 包下各 Controller 类注释里的编号一致）：</p>

> <ol>
> <li>基本注解：<code>POST /valid/basic</code></li>
> <li>四种「触发校验」的写法：<code>/valid/trigger/*</code></li>
> <li>跨字段布尔断言：<code>POST /valid/assert0</code></li>
> <li>自定义校验（单字段 / 类级别 / 依赖 Bean / 复合注解）：<code>POST /valid/custom</code></li>
> <li>Hibernate Validator 扩展注解：<code>POST /valid/hibernate</code></li>
> <li>嵌套对象 / 集合元素 / Map 的校验：<code>POST /valid/nested</code></li>
> <li>分组校验：<code>/valid/group/create | update | default</code></li>
> <li>分组顺序：<code>POST /valid/orderedGroup</code></li>
> <li>动态分组（条件必填）：<code>POST /valid/dynamicGroup</code></li>
> <li>级联分组转换：<code>/valid/convertGroup/create | update</code></li>
> <li>编程式校验：<code>POST /valid/programmatic</code></li>
> </ol>

## 1. 学习目标

掌握 Spring Boot 中参数校验的声明式用法与触发机制：清楚常见注解各自管什么、<code>@Valid</code> 和 <code>@Validated</code> 的区别、嵌套对象与集合元素怎么校验、什么时候要自定义注解或复合注解、四种分组方式（普通分组 / 分组顺序 / 动态分组 / 级联分组转换）各自解决什么问题，以及「注解不适用时」怎么注入 <code>Validator</code> 手动校验；同时能看懂校验失败后异常是怎么一路变成 <code>400 + Result</code> 的，以及为什么响应里的 message 不带字段名。

## 2. 学习清单

### 2.1 校验场景

<table>
<tr>
<th>状态</th>
<th>优先级</th>
<th>演示场景</th>
<th>覆盖的校验能力</th>
<th>入口</th>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>基本注解</td>
<td><code>@Null</code> / <code>@NotNull</code> / <code>@NotEmpty</code> / <code>@NotBlank</code> / <code>@Size</code> / <code>@Min</code> / <code>@Max</code> / <code>@DecimalMin</code> / <code>@DecimalMax</code> / <code>@Positive</code> / <code>@PositiveOrZero</code> / <code>@Negative</code> / <code>@NegativeOrZero</code> / <code>@Digits</code> / <code>@Email</code> / <code>@Pattern</code> / <code>@Past</code> / <code>@PastOrPresent</code> / <code>@Future</code> / <code>@FutureOrPresent</code> / <code>@AssertTrue</code> / <code>@AssertFalse</code></td>
<td><code>POST /valid/basic</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>四种「触发校验」的写法</td>
<td><code>@RequestBody + @Valid</code>、<code>@RequestBody + @Validated</code>、<code>@RequestParam</code> 上的约束、<code>@PathVariable</code> 上的约束；后两者在本模块（<b>没有类级 <code>@Validated</code></b>）由 Spring MVC 6.1+ 内置方法校验接管，抛 <code>HandlerMethodValidationException</code></td>
<td><code>/valid/trigger/*</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>跨字段校验（Getter 上加 <code>@AssertTrue</code>）</td>
<td>在 <code>getXxx()</code> 上写约束，把「密码 = 确认密码」「开始日期 ≤ 结束日期」这类规则变成一个布尔属性</td>
<td><code>POST /valid/assert0</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>自定义单字段注解</td>
<td><code>@Constraint(validatedBy = ...)</code> + 实现 <code>ConstraintValidator</code>（手机号 <code>@Phone</code>）</td>
<td><code>POST /valid/custom</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>类级别注解 + 把错误挂到具体字段</td>
<td><code>@Target(TYPE)</code> 的跨字段约束 <code>@PasswordMatch</code>，<code>disableDefaultConstraintViolation()</code> + <code>addPropertyNode()</code></td>
<td><code>POST /valid/custom</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>校验器里注入 Spring Bean</td>
<td><code>@UsernameUnique</code> 的校验器构造器注入 <code>UserRepository</code>（查库判重）</td>
<td><code>POST /valid/custom</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>复合注解</td>
<td><code>@ReportAsSingleViolation</code> + <code>@ConstraintComposition(AND)</code> 把 <code>@NotBlank</code>/<code>@Size</code>/<code>@Pattern</code> 合成 <code>@ValidUsername</code>，只报一条错误</td>
<td><code>POST /valid/custom</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>Hibernate Validator 扩展注解</td>
<td><code>@Length</code> / <code>@Range</code> / <code>@URL</code> / <code>@CreditCardNumber</code> / <code>@ISBN</code>（<code>@ScriptAssert</code> 依赖 JSR-223，JDK 15+ 已无 Nashorn，本模块用 <code>@AssertTrue</code> 替代，见 §4.2.3）</td>
<td><code>POST /valid/hibernate</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>嵌套对象 / 集合 / Map</td>
<td>嵌套对象加 <code>@Valid</code>；<code>List&lt;@Valid T&gt;</code>、<code>List&lt;@NotBlank String&gt;</code>、<code>Map&lt;@NotBlank K, @NotBlank V&gt;</code> 的容器元素校验</td>
<td><code>POST /valid/nested</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>分组校验</td>
<td><code>groups</code> 属性 + <code>@Validated(CreateGroup.class)</code>：同一个 DTO 在新增 / 更新场景下规则不同</td>
<td><code>/valid/group/{create,update,default}</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>分组顺序</td>
<td><code>@GroupSequence</code> 定义先校验「必填」再校验「格式」，前一组不通过就不跑后一组</td>
<td><code>POST /valid/orderedGroup</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>动态分组（条件必填）</td>
<td><code>@GroupSequenceProvider</code> + <code>DefaultGroupSequenceProvider</code>：按对象自身的字段值决定本次要跑哪些分组</td>
<td><code>POST /valid/dynamicGroup</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>级联分组转换</td>
<td><code>@ConvertGroup(from = 外部分组, to = 内部分组)</code>：让嵌套对象不沿用外层分组（HV 8 已支持重复标注）</td>
<td><code>/valid/convertGroup/create | update</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>编程式校验</td>
<td>注入 <code>Validator</code>，在 Service 里手动 <code>validate()</code>，失败抛 <code>BizException(SysErrorCode.PARAM_INVALID)</code></td>
<td><code>POST /valid/programmatic</code></td>
</tr>
<tr>
<td>⬜</td>
<td>★★☆</td>
<td>校验错误消息国际化</td>
<td><a href="../docs/参数校验错误消息国际化详设.md">详设</a>：<code>MessageSource</code> + <code>LocaleContextMessageInterpolator</code>，按 <code>Accept-Language</code> 返回不同语言的提示</td>
<td>—</td>
</tr>
<tr>
<td>⬜</td>
<td>★★☆</td>
<td>校验错误的结构化返回</td>
<td>现在只把各字段的 message 拼成一个字符串；可以返回 <code>Map&lt;字段, 消息列表&gt;</code> 让前端逐字段标红</td>
<td>—</td>
</tr>
</table>

### 2.2 校验是怎么被触发的（调用链路）

<ol>
<li><b>进入 DispatcherServlet</b>：按 URL 找到 Handler 方法，交给对应的 <code>HandlerMethodArgumentResolver</code>。</li>
<li><b><code>@RequestBody</code> 这条线</b>：<code>RequestResponseBodyMethodProcessor</code> 用 Jackson 反序列化后，若参数上有 <code>@Valid</code>/<code>@Validated</code>（或方法参数本身带约束注解），就调用 <code>WebDataBinder</code> 校验；失败抛 <code>MethodArgumentNotValidException</code>（此时还没进方法体）。</li>
<li><b><code>@RequestParam</code> / <code>@PathVariable</code> 这条线</b>：解析器只负责取值；Spring MVC 6.1+ 在调用方法前由内置的 <code>HandlerMethodValidator</code> 校验参数上的约束，失败抛 <code>HandlerMethodValidationException</code>（<b>不需要</b>类级 <code>@Validated</code>）。</li>
<li><b>类级 <code>@Validated</code> 这条线</b>：<code>MethodValidationPostProcessor</code> 给 Bean 生成 AOP 代理，方法调用前用 <code>ExecutableValidator</code> 校验，失败抛 <code>ConstraintViolationException</code>（本模块的 Controller 都没有类级 <code>@Validated</code>，这条线只作为对照）。</li>
<li><b>手动校验这条线</b>：<code>ProgrammaticService</code> 注入 <code>jakarta.validation.Validator</code>，在业务方法里 <code>validate(dto)</code>，自己收集消息、抛业务异常。</li>
<li><b>统一收口</b>：前三条线的异常都被 <a href="../00-springboot-common/README.md">00 模块</a>的 <code>GlobalExceptionHandler</code> 转换成 <code>400 + Result(SYS0004, "参数校验失败: ...")</code>；编程式校验抛的是业务异常，返回 <code>200 + SYS0004</code>。</li>
</ol>

### 2.3 模块结构

```text
05-springboot-validation
└── src/main
    ├── java/com/hyao/validation
    │   ├── ValidationApplication.java            启动类（只有 @SpringBootApplication）
    │   ├── controller                              11 个示例入口
    │   │   ├── BasicAnnotatedController.java       1. 基本注解
    │   │   ├── TriggerController.java              2. 四种触发方式
    │   │   ├── AssertController.java               3. @AssertTrue 跨字段
    │   │   ├── CustomController.java               4. 自定义 / 复合 / 依赖 Bean 的校验
    │   │   ├── HibernateValidatorController.java   5. HV 扩展注解
    │   │   ├── NestedController.java               6. 嵌套 / 集合 / Map
    │   │   ├── GroupController.java                7. 分组
    │   │   ├── OrderedGroupController.java         8. 分组顺序
    │   │   ├── DynamicGroupController.java         9. 动态分组
    │   │   ├── ConvertGroupController.java        10. 级联分组转换
    │   │   └── ProgrammaticController.java        11. 编程式校验
    │   ├── model/dto                               按示例分子包存放请求体
    │   │   ├── assert0/AssertDTO.java
    │   │   ├── basic/BasicAnnotatedDTO.java
    │   │   ├── custom/CustomDTO.java
    │   │   ├── group/{GroupDTO,ConvertGroupDTO,OrderedGroupDTO,DynamicDTO}.java
    │   │   ├── hibernate/HibernateDTO.java
    │   │   ├── nested/{BaseDTO,NestedDTO}.java
    │   │   └── trigger/TriggerDTO.java
    │   ├── repository/UserRepository.java          模拟查库（用户名 hyao 视为已存在）
    │   ├── service/ProgrammaticService.java        编程式校验
    │   └── validation
    │       ├── annotation/{Phone,UsernameUnique,PasswordMatch,ValidUsername}.java
    │       ├── group/{CreateGroup,UpdateGroup,OuterCreateGroup,OuterUpdateGroup,NotBlankGroup,ValidGroup,OtherGroup,OrderedGroup,DynamicGroupProvider}.java
    │       └── validator/{PhoneValidator,UsernameUniqueValidator,PasswordMatchValidator}.java
    └── resources/application.yaml                  只有 server.port
```

## 3. 依赖与配置

### 3.1 依赖

本模块 POM 只有**一个**直接依赖：

```xml
<dependency>
    <groupId>com.hyao.common</groupId>
    <artifactId>00-springboot-common</artifactId>
</dependency>
```

<ul>
<li><code>00-springboot-common</code> 自己声明了 <code>spring-boot-starter-validation</code>（Bean Validation API + Hibernate Validator），所以本模块能直接用 <code>jakarta.validation.constraints.*</code>、<code>org.hibernate.validator.constraints.*</code>；</li>
<li><code>spring-boot-starter-web</code>、Jackson、Lombok、<code>commons-lang3</code> 来自父 POM 的公共 <code>&lt;dependencies&gt;</code>，子模块自动继承，所以本模块 POM 里看不到；</li>
<li>版本由父 POM 的 <code>spring-boot-dependencies</code> BOM 管理：Spring Boot 3.5.16 → Hibernate Validator <b>8.0.3.Final</b>、Jakarta Validation API <b>3.0.2</b>，子模块不写 <code>&lt;version&gt;</code>。</li>
</ul>

### 3.2 配置

校验本身**没有专属配置项**，规则都写在注解里。本模块的 <code>application.yaml</code> 只有端口：

```yaml
server:
  port: 8085
```

和「校验结果长什么样」相关的常用配置如下（都在默认值下工作，本模块没有额外打开）：

<table>
<tr>
<th>配置项</th>
<th>作用</th>
<th>本模块取值</th>
</tr>
<tr>
<td><code>server.port</code></td>
<td>端口</td>
<td><code>8085</code>（01/02/03/04 分别是 8081/8082/8083/8084）</td>
</tr>
<tr>
<td><code>spring.jackson.time-zone</code> / <code>spring.jackson.date-format</code></td>
<td>JSON 里日期时间的序列化格式；DTO 字段上也可以用 <code>@JsonFormat</code> 单独指定（本模块 <code>AssertDTO</code> 用的是注解）</td>
<td>未配置，用 Jackson 默认</td>
</tr>
<tr>
<td><code>spring.jackson.deserialization.fail-on-unknown-properties</code></td>
<td>请求体里多传 DTO 没有的字段时是否报错</td>
<td>未配置（默认 <code>false</code>：忽略多余字段）</td>
</tr>
<tr>
<td><code>logging.level.org.hibernate.validator</code></td>
<td>看校验器初始化、约束解析的调试日志</td>
<td>未配置</td>
</tr>
<tr>
<td><code>server.error.path</code></td>
<td>容器错误页路径，00 的 <code>CommonErrorController</code> 按它兜底未匹配的 4xx</td>
<td>未配置（默认 <code>/error</code>）</td>
</tr>
</table>

### 3.3 校验失败的响应约定

校验失败统一走 00 模块的 <code>GlobalExceptionHandler</code>。**HTTP 状态码和响应体 <code>code</code> 是两套语义**，且 <code>message</code> 里<b>只有注解的 message 原文、不带字段路径</b>（00 的实现只取 <code>getDefaultMessage()</code> 并做 <code>distinct</code> 去重）：

<table>
<tr>
<th>场景</th>
<th>异常</th>
<th>HTTP</th>
<th>响应体 <code>code</code></th>
<th>message 形态（实测）</th>
</tr>
<tr>
<td><code>@Valid @RequestBody</code> 校验失败</td>
<td><code>MethodArgumentNotValidException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败: 新增时 id 必须为空</code>（多字段用 <code>; </code> 拼接，重复消息去重）</td>
</tr>
<tr>
<td>单参数（<code>@RequestParam</code> / <code>@PathVariable</code>）约束失败，且类上没有 <code>@Validated</code></td>
<td><code>HandlerMethodValidationException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败: id 不能超过 100</code></td>
</tr>
<tr>
<td>类上标了 <code>@Validated</code> 时的单参数失败</td>
<td><code>ConstraintViolationException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td>同 00 README 的示例（本模块当前没有类级 <code>@Validated</code>）</td>
</tr>
<tr>
<td>请求体不是合法 JSON / 字段类型不匹配</td>
<td><code>HttpMessageNotReadableException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败，请求体解析失败</code></td>
</tr>
<tr>
<td>缺必填的 <code>@RequestParam</code></td>
<td><code>MissingServletRequestParameterException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败，缺少参数: xxx</code></td>
</tr>
<tr>
<td>编程式校验抛 <code>BizException(PARAM_INVALID)</code></td>
<td><code>BizException</code></td>
<td><code>200</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败</code>（明细只在日志里，便于区分「请求参数不合法」和「业务失败」）</td>
</tr>
<tr>
<td>其它未预期异常</td>
<td><code>Exception</code></td>
<td><code>200</code></td>
<td><code>SYS0001</code></td>
<td><code>系统内部异常</code></td>
</tr>
</table>

## 4. 核心实现

### 4.1 启动配置：为什么「加个依赖 + 加个注解」就生效

<ul>
<li><b>启动类</b>：<code>ValidationApplication</code> 只有 <code>@SpringBootApplication</code>，没有 <code>@EnableWebMvc</code>、没有 <code>@EnableMethodValidation</code>。</li>
<li><b>校验器从哪来</b>：Boot 的 <code>ValidationAutoConfiguration</code> 发现 classpath 上有 Hibernate Validator，就注册 <code>LocalValidatorFactoryBean</code>（同时是 Spring <code>Validator</code> 和 Bean Validation <code>ValidatorFactory</code>），并作为 <code>@Primary</code> 的 <code>Validator</code> Bean——所以 <code>ProgrammaticService</code> 可以直接构造器注入 <code>jakarta.validation.Validator</code>。</li>
<li><b><code>@RequestBody</code> 怎么接上</b>：<code>WebMvcAutoConfiguration</code> 把这个 <code>Validator</code> 装配进 <code>WebDataBinder</code>，<code>RequestResponseBodyMethodProcessor</code> 解析完请求体后调用校验。</li>
<li><b>单参数怎么接上</b>：Spring MVC 6.1+ 内置 <code>HandlerMethodValidator</code>，默认开启；控制器方法参数上的约束（<code>@RequestParam</code>、<code>@PathVariable</code>、<code>@RequestHeader</code> 等）由它在调用前校验。<b>不需要</b>类级 <code>@Validated</code>，加了反而会切到 AOP 代理那条链路（抛 <code>ConstraintViolationException</code>）。</li>
<li><b>异常收口</b>：00 模块的 <code>CommonWebAutoConfiguration</code> 通过 <code>AutoConfiguration.imports</code> 注册 <code>GlobalExceptionHandler</code>（继承 <code>ResponseEntityExceptionHandler</code>），本模块不写任何 try-catch 就得到结构化错误。</li>
</ul>

### 4.2 具体实现

#### 4.2.1 基本注解：`BasicAnnotatedDTO`

[`BasicAnnotatedDTO`](src/main/java/com/hyao/validation/model/dto/basic/BasicAnnotatedDTO.java) 把 JSR-380 内置注解基本列了一遍（26 个约束字段），每个字段一条注释说明语义。类注释里有三条要点：

<ul>
<li>注解的默认错误消息是预定义的，示例里统一用 <code>message</code> 覆盖成中文；</li>
<li>除 <code>@NotNull</code>/<code>@NotEmpty</code>/<code>@NotBlank</code> 外，其余注解<b>对 <code>null</code> 默认放行</b>，要判空必须再显式加 <code>@NotNull</code>；</li>
<li><code>private boolean fieldBoolean</code> 加 <code>@AssertTrue</code>：布尔字段的 getter 是 <code>isFieldBoolean()</code>，约束加在字段上等价于对 <code>true</code> 的断言。</li>
</ul>

实测（全空 body）：<code>fieldNotEmptyMap 映射不能为空; fieldNotBlank 字符串不能为 null / 空字符串 / 空格; fieldNotNull 字段不能为 null; fieldNotEmptyList 列表不能为空; fieldBoolean 字段值必须为 true; fieldNotEmpty 字符串不能为 null / 空字符串</code>——顺带能看出 <b>多字段错误的顺序不保证稳定</b>。

#### 4.2.2 四种「触发校验」的写法：`TriggerController`

[`TriggerController`](src/main/java/com/hyao/validation/controller/TriggerController.java) 一个类里放四种写法：

<table>
<tr>
<th>接口</th>
<th>写法</th>
<th>失败时的异常</th>
</tr>
<tr>
<td><code>POST /valid/trigger/body1</code></td>
<td><code>@Valid @RequestBody TriggerDTO</code></td>
<td><code>MethodArgumentNotValidException</code></td>
</tr>
<tr>
<td><code>POST /valid/trigger/body2</code></td>
<td><code>@Validated @RequestBody TriggerDTO</code></td>
<td><code>MethodArgumentNotValidException</code>（对请求体来说 <code>@Validated</code> 与 <code>@Valid</code> 等价）</td>
</tr>
<tr>
<td><code>GET /valid/trigger/param?id=111</code></td>
<td><code>@RequestParam @Max(100) Integer id</code></td>
<td><code>HandlerMethodValidationException</code>（类上<b>没有</b> <code>@Validated</code>）</td>
</tr>
<tr>
<td><code>GET /valid/trigger/path/111</code></td>
<td><code>@PathVariable @Max(100) Integer id</code></td>
<td><code>HandlerMethodValidationException</code></td>
</tr>
</table>

实测：四个接口的 <code>123</code> / <code>111</code> 都返回 <code>400</code>，message 分别是 <code>参数校验失败: id 长度不能超过 100</code>（body 里 <code>@Max</code> 的 message 写的是「id 长度不能超过 100」）和 <code>参数校验失败: id 不能超过 100</code>（param / path）。

<b>要点</b>：单参数校验是否生效，取决于「约束注解 + 参数」本身，<b>不</b>取决于类级 <code>@Validated</code>；类级 <code>@Validated</code> 只是把触发者从 Spring MVC 换成 AOP 代理。

#### 4.2.3 跨字段布尔断言：`AssertDTO`

[`AssertDTO`](src/main/java/com/hyao/validation/model/dto/assert0/AssertDTO.java) 用「Getter 上加 <code>@AssertTrue</code>」的方式做跨字段校验：

<ul>
<li><code>getPasswordMatch()</code>：只有两个字段都非空时才比较，相等返回 <code>true</code>；</li>
<li><code>getDateValid()</code>：<code>begin</code> 不晚于 <code>end</code> 时返回 <code>true</code>；</li>
<li>两个方法都标了 <code>@JsonIgnore</code>，避免被 Jackson 当成响应字段序列化；</li>
<li>方法必须符合 JavaBean Getter 规范（<code>getXxx</code> / <code>isXxx</code>、无参、返回 <code>boolean</code>/<code>Boolean</code>），否则 HV 直接忽略。</li>
</ul>

实测：密码不一致 + 日期倒置 → <code>400</code>，message <code>参数校验失败: 密码不一致; 开始日期不能在结束日期之后</code>。

> 这也是 <code>@ScriptAssert</code> 的替代方案：HV 8.0.3 里 <code>@ScriptAssert</code> 类还存在，但它需要 JSR-223 脚本引擎，而 JDK 15+ 移除了 Nashorn，运行时必然 <code>HV000170</code>。

#### 4.2.4 自定义校验：`CustomDTO` + `validation` 包

[`CustomDTO`](src/main/java/com/hyao/validation/model/dto/custom/CustomDTO.java) 一个 DTO 演示四种自定义能力：

<table>
<tr>
<th>能力</th>
<th>注解 / 校验器</th>
<th>核心写法</th>
</tr>
<tr>
<td>单字段自定义约束</td>
<td><code>@Phone</code> → <code>PhoneValidator</code></td>
<td>注解上 <code>@Constraint(validatedBy = PhoneValidator.class)</code>；校验器实现 <code>ConstraintValidator&lt;Phone, String&gt;</code>，<code>null</code> 视为合法（交给 <code>@NotBlank</code>）</td>
</tr>
<tr>
<td>依赖 Spring Bean 的校验器</td>
<td><code>@UsernameUnique</code> → <code>UsernameUniqueValidator</code></td>
<td>校验器<b>不加 <code>@Component</code></b>，用构造器注入 <code>UserRepository</code>；HV 创建校验器实例时由 Spring 的 <code>ConstraintValidatorFactory</code> 完成依赖注入</td>
</tr>
<tr>
<td>类级别跨字段约束 + 错误挂到字段</td>
<td><code>@PasswordMatch</code> → <code>PasswordMatchValidator</code></td>
<td>注解 <code>@Target(TYPE)</code>；校验器里 <code>context.disableDefaultConstraintViolation()</code>，再 <code>buildConstraintViolationWithTemplate(msg).addPropertyNode("confirmPassword").addConstraintViolation()</code></td>
</tr>
<tr>
<td>复合注解</td>
<td><code>@ValidUsername</code></td>
<td><code>@NotBlank</code> + <code>@Size(2,10)</code> + <code>@Pattern(^\w+$)</code> + <code>@ReportAsSingleViolation</code> + <code>@ConstraintComposition(AND)</code>，只报一条「用户名只能包含字母数字下划线，且长度限定 2~10 位」</td>
</tr>
</table>

实测（用户名 <code>hyao</code>、手机号 <code>1341123</code>、两次密码不同）：<code>400</code>，message <code>参数校验失败: 两次输入的密码不一致; 手机号码格式不正确; 用户名已存在</code>。

#### 4.2.5 Hibernate Validator 扩展注解：`HibernateDTO`

[`HibernateDTO`](src/main/java/com/hyao/validation/model/dto/hibernate/HibernateDTO.java) 用了 `org.hibernate.validator.constraints` 下的 5 个扩展注解（8.0.3 中均仍存在）：

<ul>
<li><code>@Length(min=1, max=10)</code>：字符串长度，语义比 <code>@Size</code> 更明确；</li>
<li><code>@Range(min=0, max=10)</code>：等价于 <code>@Min(0) + @Max(10)</code>（注意 <code>min=0</code>，所以传 <code>0</code> 合法）；</li>
<li><code>@URL(protocol="https", host="example.com")</code>：URL 结构校验；</li>
<li><code>@CreditCardNumber</code>：Luhn 算法校验，能过滤随意敲的数字串；</li>
<li><code>@ISBN(type = ISBN.Type.ANY)</code>：ISBN 书号。</li>
</ul>

实测（<code>l=""</code>、<code>r=15</code>、<code>url="http://x.com"</code>、<code>creditCard="123"</code>、<code>isbn="123"</code>）→ <code>400</code>，message <code>参数校验失败: URL 必须是 https 协议，主机是 example.com; 信用卡号必须符合格式; r 数值必须在 1 到 10 之间; ISBN 书号必须符合格式; l 长度必须在 1 到 10 之间</code>。

#### 4.2.6 嵌套对象 / 集合元素 / Map：`BaseDTO`

[`BaseDTO`](src/main/java/com/hyao/validation/model/dto/nested/BaseDTO.java) 覆盖三种「递归校验」写法：

<ul>
<li><code>@NotNull + @Valid private NestedDTO nested</code>：<code>@NotNull</code> 管对象本身，<code>@Valid</code> 才会把校验递归进 <code>NestedDTO.id</code>；</li>
<li><code>@Valid @NotEmpty private List&lt;NestedDTO&gt; list1</code>：<code>@Valid</code> 标在字段上，对集合<b>每个元素</b>生效；</li>
<li><code>@NotEmpty private List&lt;@Valid NestedDTO&gt; list2</code>：<code>@Valid</code> 标在<b>类型参数</b>上，效果等价；</li>
<li><code>private List&lt;@NotBlank(message="集合中的元素不能为空") String&gt; list3</code>：只校验元素本身，不涉及对象；</li>
<li><code>Map&lt;@NotBlank(message="键不能为空") String, @NotBlank(message="值不能为空") String&gt; map</code>：键和值都是容器元素，可以分别加约束。</li>
</ul>

实测 body <code>{"nested":{"id":"12345"},"list1":[{"id":"123456"}],"list2":[{"id":"123456"}],"list3":["111",""],"map":{"id":""}}</code> → <code>400</code>，message <code>参数校验失败: id 长度不能超过 4; 值不能为空; 集合中的元素不能为空</code>。三条重复的「id 长度不能超过 4」（nested / list1 / list2）被 00 的 <code>distinct()</code> 合并成一条——这是 message 不带路径的副作用。

#### 4.2.7 分组校验：`GroupDTO` + `GroupController`

<table>
<tr>
<th>接口</th>
<th>分组</th>
<th>生效的约束</th>
<th>实测失败消息</th>
</tr>
<tr>
<td><code>POST /valid/group/create</code></td>
<td><code>@Validated(CreateGroup.class)</code></td>
<td><code>@Null(groups=CreateGroup)</code>、<code>@NotBlank(groups={Create,Update})</code></td>
<td>id=123 → <code>新增时 id 必须为空</code></td>
</tr>
<tr>
<td><code>POST /valid/group/update</code></td>
<td><code>@Validated(UpdateGroup.class)</code></td>
<td><code>@NotNull(groups=UpdateGroup)</code>、<code>@NotBlank(groups={Create,Update})</code></td>
<td>缺 id → <code>更新时 id 不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/group/default</code></td>
<td><code>@Validated</code>（Default 分组）</td>
<td>只有 <code>remark</code> 的 <code>@NotBlank</code>（未写 groups）</td>
<td>remark="" → <code>备注不能为空</code>；此时 id/name 的分组约束<b>不跑</b></td>
</tr>
</table>

要点：**自定义分组不会继承 <code>Default</code>**。所以 <code>CreateGroup</code> / <code>UpdateGroup</code> 场景下，没写 groups 的 <code>remark</code> 约束不会执行；反过来 <code>Default</code> 分组也不会跑带 groups 的约束。类注释和 <code>Groups</code> 接口都是空标记接口，只是「标签」。

#### 4.2.8 分组顺序：`OrderedGroup` + `OrderedGroupDTO`

<ul>
<li><code>OrderedGroup</code> 接口上标 <code>@GroupSequence({NotBlankGroup.class, ValidGroup.class})</code>，声明「先校验必填、再校验格式」；</li>
<li><code>OrderedGroupDTO</code>：<code>id</code> / <code>address</code> 属于 <code>NotBlankGroup</code>，<code>name</code> / <code>phone</code> 属于 <code>ValidGroup</code>；</li>
<li>控制器方法参数上写 <code>@Validated(OrderedGroup.class) @RequestBody OrderedGroupDTO</code>（本模块没有类级 <code>@Validated</code>）；</li>
<li><b>前一组不通过就短路</b>，后一组不再执行。</li>
</ul>

实测：body <code>{}</code> → <code>400</code>，只有 <code>地址不能为空; ID不能为空</code>；body 两组都不合法（<code>name</code> 超 6 位、<code>phone</code> 超 11 位）→ 只有 <code>名称长度不能超过 6; 手机号长度不能超过 11</code>。

#### 4.2.9 动态分组：`DynamicDTO` + `DynamicGroupProvider`

<ul>
<li><code>DynamicDTO</code> 类上标 <code>@GroupSequenceProvider(DynamicGroupProvider.class)</code>；</li>
<li><code>DynamicGroupProvider</code> 实现 <code>DefaultGroupSequenceProvider&lt;DynamicDTO&gt;</code>，在 <code>getValidationGroups()</code> 里返回本次要跑的分组列表；</li>
<li><b>必须把 DTO 自身加进列表</b>（代表 Default 分组），否则 HV 报错或丢掉默认约束；</li>
<li>示例规则：<code>type</code> 默认分组必填；<code>type == "OTHER"</code> 时追加 <code>OtherGroup</code>，此时 <code>description</code> 必填。</li>
</ul>

实测：<code>{"type":"OTHER","description":""}</code> → <code>400</code>，message <code>说明不能为空</code>；<code>{"type":"1"}</code> → <code>200</code>。

#### 4.2.10 级联分组转换：`ConvertGroupDTO` + `ConvertGroupController`

<ul>
<li>外层接口用 <code>@Validated(OuterCreateGroup.class)</code> / <code>@Validated(OuterUpdateGroup.class)</code>；</li>
<li>嵌套字段上标 <code>@Valid</code> + <b>两个</b> <code>@ConvertGroup</code>：<code>OuterCreateGroup → CreateGroup</code>、<code>OuterUpdateGroup → UpdateGroup</code>；</li>
<li>作用：不转换时嵌套对象会沿用外层分组（<code>GroupDTO</code> 里没有 <code>OuterCreateGroup</code> 的约束 → 一条都不报）；转换后内层按 <code>CreateGroup</code> / <code>UpdateGroup</code> 校验。</li>
</ul>

实测（HV 8.0.3）：<code>/convertGroup/create</code> 传 <code>{"dto":{"id":123,...}}</code> → <code>400 新增时 id 必须为空</code>；<code>/convertGroup/update</code> 不传 id → <code>400 更新时 id 不能为空</code>。<b>两个分组都正常</b>——这说明 HV 8 已支持重复 <code>@ConvertGroup</code>（6.x 会把两个注解合并成 <code>@ConvertGroup.List</code> 并静默忽略，是当年的经典坑，见 §4.4）。

#### 4.2.11 编程式校验：`ProgrammaticService`

<ul>
<li>构造器注入 <code>jakarta.validation.Validator</code>（Boot 自动注册的 <code>LocalValidatorFactoryBean</code>）；</li>
<li><code>validator.validate(dto)</code> 返回 <code>Set&lt;ConstraintViolation&lt;CustomDTO&gt;&gt;</code>，把 message 用 <code>"; "</code> 拼起来写日志；</li>
<li>有违规就 <code>throw new BizException(SysErrorCode.PARAM_INVALID)</code>，最终响应 <code>200 + SYS0004</code>（业务异常语义：请求本身没坏，是业务规则不接受）。</li>
</ul>

实测：<code>{"username":"","phone":"1","password":"a","confirmPassword":"b"}</code> → <code>200</code> + <code>{"code":"SYS0004","message":"参数校验失败","data":null}</code>，字段级明细只在 <code>logs/app.log</code> 里。

### 4.3 依赖之间怎么组合关联的

#### 4.3.1 三方分工

<table>
<tr>
<th>层</th>
<th>提供方</th>
<th>本模块用到的能力</th>
</tr>
<tr>
<td>标准 API</td>
<td><code>jakarta.validation-api</code>（3.0.2）</td>
<td><code>@Valid</code>、<code>@Validated</code>、<code>groups</code>、<code>@GroupSequence</code>、<code>@ConvertGroup</code>、<code>ConstraintValidator</code></td>
</tr>
<tr>
<td>实现</td>
<td><code>hibernate-validator</code>（8.0.3.Final）</td>
<td>所有内置约束的实现、<code>org.hibernate.validator.constraints.*</code>、<code>@GroupSequenceProvider</code>、<code>DefaultGroupSequenceProvider</code></td>
</tr>
<tr>
<td>Spring 集成</td>
<td>Spring Boot / Spring MVC</td>
<td>自动注册 <code>LocalValidatorFactoryBean</code>、把 <code>Validator</code> 接进 <code>WebDataBinder</code>、6.1+ 内置 <code>HandlerMethodValidator</code>、<code>MethodValidationPostProcessor</code>（AOP）、<code>@Validated</code> 参数分组提示</td>
</tr>
<tr>
<td>收口</td>
<td>00-springboot-common</td>
<td><code>GlobalExceptionHandler</code> 把四类校验异常统一成 <code>400 + SYS0004</code>；<code>ResponseWrapperAdvice</code> 把 <code>Result</code> 原样返回</td>
</tr>
</table>

#### 4.3.2 三条异常链路对比

<table>
<tr>
<th>触发方式</th>
<th>谁在调用校验</th>
<th>抛出的异常</th>
<th>message 里带路径吗</th>
</tr>
<tr>
<td><code>@Valid @RequestBody</code></td>
<td><code>RequestResponseBodyMethodProcessor</code> → <code>WebDataBinder</code> → HV</td>
<td><code>MethodArgumentNotValidException</code></td>
<td>不带（00 只取 defaultMessage）</td>
</tr>
<tr>
<td><code>@RequestParam</code> / <code>@PathVariable</code> 上的约束，类无 <code>@Validated</code></td>
<td>Spring MVC 6.1+ 的 <code>HandlerMethodValidator</code></td>
<td><code>HandlerMethodValidationException</code></td>
<td>不带</td>
</tr>
<tr>
<td>类上标 <code>@Validated</code> 后，方法参数上的约束</td>
<td><code>MethodValidationPostProcessor</code> 生成的 AOP 代理 → <code>ExecutableValidator</code></td>
<td><code>ConstraintViolationException</code></td>
<td>带（形如 <code>byParam.id</code>）</td>
</tr>
</table>

### 4.4 容易踩坑的点

<ol>
<li><b>message 不带字段名</b>：00 的实现只取 <code>getDefaultMessage()</code>，所以注解的 message 必须自带字段含义（例如「id 不能超过 100」而不是「不能超过 100」）；要让前端逐字段标红，需要做「结构化返回」（§2.1 的 ⬜ 项）。</li>
<li><b><code>@Valid</code> 只加在最外层不够</b>：嵌套对象/集合元素上没有 <code>@Valid</code> 时，只校对象是否为 <code>null</code>/集合是否为空，内部约束不执行。</li>
<li><b>自定义分组不含 <code>Default</code></b>：<code>@Validated(CreateGroup.class)</code> 时，没写 groups 的约束（如 <code>remark</code>）不会执行；需要就把它显式放进 groups。</li>
<li><b><code>@GroupSequence</code> 会短路</b>：第一组有错就不再跑后面的组，所以「两组都错时只看到一组错误」是预期行为。</li>
<li><b>动态分组必须把 DTO 自身加入序列</b>：<code>DefaultGroupSequenceProvider</code> 返回的列表里少了 DTO 类，默认分组约束就会丢。</li>
<li><b>单参数校验不需要类级 <code>@Validated</code></b>（Spring 6.1+）；反过来，类上加了 <code>@Validated</code> 会把异常类型从 <code>HandlerMethodValidationException</code> 换成 <code>ConstraintViolationException</code>，两者的 message 形态不同。</li>
<li><b><code>@ScriptAssert</code> 在 JDK 15+ 不可用</b>：类还在，但 JSR-223 脚本引擎（Nashorn）被移除，运行时抛 <code>HV000170</code>；跨字段断言改用 <code>@AssertTrue</code>。</li>
<li><b><code>@AssertTrue</code> 要挂在布尔 Getter 上</b>：方法名必须符合 JavaBean 规范（<code>getXxx</code>/<code>isXxx</code>），返回 <code>boolean</code>/<code>Boolean</code>，且记得加 <code>@JsonIgnore</code>。</li>
<li><b>校验器里 <code>null</code> 默认放行</b>：<code>ConstraintValidator</code> 的 <code>isValid(null, ctx)</code> 要自己决定返回 <code>true</code> 还是 <code>false</code>，通常返回 <code>true</code> 交给 <code>@NotBlank</code>。</li>
<li><b>重复 <code>@ConvertGroup</code></b>：HV 6.x 只识别直接标注的那个（另一个被合并进 <code>@ConvertGroup.List</code> 后忽略），表现为「级联分组转换静默失效」；8.0.3 已修复（实测两个分组都生效）。</li>
</ol>

## 5. 快速验证

前置：MySQL 不需要；JDK 27 + Maven 构建 <code>00</code> 和 <code>05</code>，然后启动 <code>ValidationApplication</code>（<code>mvn -pl 05-springboot-validation -am install</code> 后直接 IDEA 运行，或用 <code>java -cp ... com.hyao.validation.ValidationApplication</code>），端口 8085。

```bash
# 1. 基本注解：空对象（错误顺序不固定，多字段用 ; 拼接）
curl -s -X POST http://localhost:8085/valid/basic -H 'Content-Type: application/json' -d '{}'

# 2. 四种触发方式
curl -s -X POST http://localhost:8085/valid/trigger/body1 -H 'Content-Type: application/json' -d '{"id":123}'
curl -s -X POST http://localhost:8085/valid/trigger/body2 -H 'Content-Type: application/json' -d '{"id":123}'
curl -s "http://localhost:8085/valid/trigger/param?id=111"
curl -s "http://localhost:8085/valid/trigger/path/111"

# 3. 跨字段 @AssertTrue：密码不一致 + 日期倒置
curl -s -X POST http://localhost:8085/valid/assert0 -H 'Content-Type: application/json' \
  -d '{"password":"a","confirmPassword":"b","begin":"2026-12-01","end":"2026-11-01"}'

# 4. 自定义校验：用户名已存在(hyao) + 手机号格式错 + 两次密码不一致
curl -s -X POST http://localhost:8085/valid/custom -H 'Content-Type: application/json' \
  -d '{"username":"hyao","phone":"1341123","password":"a","confirmPassword":"b"}'

# 5. Hibernate Validator 扩展注解（@Range min=0，r 传 15 才违规）
curl -s -X POST http://localhost:8085/valid/hibernate -H 'Content-Type: application/json' \
  -d '{"l":"","r":15,"url":"http://x.com","creditCard":"123","isbn":"123"}'

# 6. 嵌套 / 集合 / Map
curl -s -X POST http://localhost:8085/valid/nested -H 'Content-Type: application/json' \
  -d '{"nested":{"id":"12345"},"list1":[{"id":"123456"}],"list2":[{"id":"123456"}],"list3":["111",""],"map":{"id":""}}'

# 7. 分组：create 要求 id 为空；update 要求 id 非空；default 只校 remark
curl -s -X POST http://localhost:8085/valid/group/create  -H 'Content-Type: application/json' -d '{"id":123,"name":"hyao","remark":""}'
curl -s -X POST http://localhost:8085/valid/group/update  -H 'Content-Type: application/json' -d '{"name":"hyao","remark":""}'
curl -s -X POST http://localhost:8085/valid/group/default -H 'Content-Type: application/json' -d '{"id":123,"name":"hyao","remark":""}'

# 8. 分组顺序：两组都不通过时只返回第一组
curl -s -X POST http://localhost:8085/valid/orderedGroup -H 'Content-Type: application/json' \
  -d '{"id":"1","name":"aaacccs","address":"345","phone":"111222333444"}'

# 9. 动态分组：type=OTHER 时 description 必填
curl -s -X POST http://localhost:8085/valid/dynamicGroup -H 'Content-Type: application/json' -d '{"type":"OTHER","description":""}'

# 10. 级联分组转换
curl -s -X POST http://localhost:8085/valid/convertGroup/create -H 'Content-Type: application/json' -d '{"dto":{"id":123,"name":"hyao","remark":""}}'
curl -s -X POST http://localhost:8085/valid/convertGroup/update -H 'Content-Type: application/json' -d '{"dto":{"name":"hyao","remark":""}}'

# 11. 编程式校验（业务异常，HTTP 200）
curl -s -X POST http://localhost:8085/valid/programmatic -H 'Content-Type: application/json' \
  -d '{"username":"","phone":"1","password":"a","confirmPassword":"b"}'
```

实测结果（2026-10-31，JDK 27 + Spring Boot 3.5.16 + HV 8.0.3）：

<table>
<tr>
<th>#</th>
<th>请求</th>
<th>HTTP</th>
<th>code</th>
<th>message</th>
</tr>
<tr>
<td>1</td>
<td><code>POST /valid/basic</code> <code>{}</code></td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: fieldNotEmptyMap 映射不能为空; fieldNotBlank 字符串不能为 null / 空字符串 / 空格; fieldNotNull 字段不能为 null; fieldNotEmptyList 列表不能为空; fieldBoolean 字段值必须为 true; fieldNotEmpty 字符串不能为 null / 空字符串</code></td>
</tr>
<tr>
<td>2</td>
<td><code>POST /valid/trigger/body1</code> <code>{"id":123}</code></td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: id 长度不能超过 100</code></td>
</tr>
<tr>
<td>3</td>
<td><code>GET /valid/trigger/param?id=111</code></td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: id 不能超过 100</code></td>
</tr>
<tr>
<td>4</td>
<td><code>GET /valid/trigger/path/111</code></td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: id 不能超过 100</code></td>
</tr>
<tr>
<td>5</td>
<td><code>POST /valid/assert0</code> 密码不一致 + 日期倒置</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 密码不一致; 开始日期不能在结束日期之后</code></td>
</tr>
<tr>
<td>6</td>
<td><code>POST /valid/custom</code> 用户名 hyao</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 两次输入的密码不一致; 手机号码格式不正确; 用户名已存在</code></td>
</tr>
<tr>
<td>7</td>
<td><code>POST /valid/hibernate</code> 全非法</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: URL 必须是 https 协议，主机是 example.com; 信用卡号必须符合格式; r 数值必须在 1 到 10 之间; ISBN 书号必须符合格式; l 长度必须在 1 到 10 之间</code></td>
</tr>
<tr>
<td>8</td>
<td><code>POST /valid/nested</code></td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: id 长度不能超过 4; 值不能为空; 集合中的元素不能为空</code>（重复消息已去重）</td>
</tr>
<tr>
<td>9</td>
<td><code>POST /valid/group/create</code> id=123</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 新增时 id 必须为空</code></td>
</tr>
<tr>
<td>10</td>
<td><code>POST /valid/group/update</code> 缺 id</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 更新时 id 不能为空</code></td>
</tr>
<tr>
<td>11</td>
<td><code>POST /valid/group/default</code> remark=""</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 备注不能为空</code></td>
</tr>
<tr>
<td>12</td>
<td><code>POST /valid/orderedGroup</code> 两组都不合法</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 名称长度不能超过 6; 手机号长度不能超过 11</code></td>
</tr>
<tr>
<td>13</td>
<td><code>POST /valid/dynamicGroup</code> type=OTHER</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 说明不能为空</code></td>
</tr>
<tr>
<td>14</td>
<td><code>POST /valid/convertGroup/create</code> id=123</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 新增时 id 必须为空</code></td>
</tr>
<tr>
<td>15</td>
<td><code>POST /valid/convertGroup/update</code> 缺 id</td>
<td>400</td>
<td>SYS0004</td>
<td><code>参数校验失败: 更新时 id 不能为空</code></td>
</tr>
<tr>
<td>16</td>
<td><code>POST /valid/programmatic</code></td>
<td>200</td>
<td>SYS0004</td>
<td><code>参数校验失败</code>（明细在日志）</td>
</tr>
</table>

## 6. 常见问题与踩坑

<table>
<tr>
<th>问题</th>
<th>原因 / 解法</th>
</tr>
<tr>
<td>响应 message 里没有字段名，前端没法标红</td>
<td>00 的 handler 只取 <code>getDefaultMessage()</code>；要么在 message 里写清字段（本项目做法），要么做结构化返回（<a href="../docs/参数校验错误消息国际化详设.md">详设</a> 的姊妹项，见 §2.1 ⬜）</td>
</tr>
<tr>
<td>给 <code>@RequestParam</code> 加了 <code>@Max</code> 但不生效</td>
<td>Spring 6.1+ 会校验，前提是参数解析器认得约束；若仍不生效，检查注解是否落在 <code>@RequestParam</code> 参数上、以及 <code>spring-boot-starter-validation</code> 是否在 classpath</td>
</tr>
<tr>
<td>类上加了 <code>@Validated</code> 后异常类型变了</td>
<td>变成 AOP 代理校验 → <code>ConstraintViolationException</code>（message 带 <code>方法名.参数名</code> 路径）；两者都被 00 的 handler 接住，只是 message 形态不同</td>
</tr>
<tr>
<td><code>@Valid</code> 加了但嵌套对象的约束没跑</td>
<td>只有最外层加了 <code>@Valid</code> 不够，嵌套字段/集合元素上必须也有 <code>@Valid</code>（或 <code>List&lt;@Valid T&gt;</code>）</td>
</tr>
<tr>
<td>自定义分组里 <code>@NotBlank</code> 不生效</td>
<td>自定义分组不继承 <code>Default</code>；没写 groups 的约束只在默认分组生效</td>
</tr>
<tr>
<td>分组顺序里后面的组永远不报错</td>
<td><code>@GroupSequence</code> 是短路语义，前一组有错就停；要一次看全部错误就不要用序列</td>
</tr>
<tr>
<td>动态分组报「没有 Default 分组」之类的错</td>
<td><code>DefaultGroupSequenceProvider</code> 返回的列表里必须包含 DTO 自身（代表 Default）</td>
</tr>
<tr>
<td>两个 <code>@ConvertGroup</code> 只有一个生效</td>
<td>HV 6.x 的重复注解 bug；升级到 HV 8（本项目已是 8.0.3）即正常（已实测）</td>
</tr>
<tr>
<td>JDK 21+ 下 <code>@ScriptAssert</code> 报 <code>HV000170</code></td>
<td>Nashorn 被移除，改用 <code>@AssertTrue</code> 的 Getter 断言</td>
</tr>
<tr>
<td>校验器里注入的 Bean 是 <code>null</code></td>
<td>校验器不要加 <code>@Component</code>；依赖由 Spring 的 <code>ConstraintValidatorFactory</code> 注入（构造器注入即可）</td>
</tr>
</table>

## 7. 学习笔记

<ul>
<li><b>声明式优先，编程式兜底</b>：能用注解表达的就别写 <code>if</code>；注解表达不了的跨字段规则用 <code>@AssertTrue</code> 或类级约束；只在「非 Web 入口/第三方对象」场景才注入 <code>Validator</code> 手动校验。</li>
<li><b>校验是「请求边界」上的事</b>：DTO 上把规则写全，Service 里就不用再判空；校验失败属于 400 语义，业务规则失败属于 200 + 业务码。</li>
<li><b>四种分组的用途不同</b>：<code>groups</code> 区分场景；<code>@GroupSequence</code> 控制顺序并短路；<code>@GroupSequenceProvider</code> 按数据动态决定；<code>@ConvertGroup</code> 解决嵌套对象分组继承。</li>
<li><b>错误信息是协议的一部分</b>：message 会被前端直接展示，要写「字段 + 原因」；国际化是下一步（<a href="../docs/参数校验错误消息国际化详设.md">详设</a>）。</li>
<li><b>升级 Boot 3 之后要重看触发链路</b>：Spring 6.1+ 把单参数校验收进 MVC（<code>HandlerMethodValidationException</code>），<code>@Validated</code> 不再是必需品，异常类型也随之变化。</li>
</ul>
