# 05-springboot-validation：Spring Boot 参数校验

> ⚠️ **本 README 由 AI 辅助生成后人工优化并完善补充**（随代码变更同步更新），内容按实际代码与实跑结果核对过，但仍可能有疏漏或滞后，**如与代码不一致请以源码为准**。

## 这个模块做什么

这个模块用若干最小示例，演示 Spring Boot 里 **参数校验（Bean Validation）** 的完整用法：从「一个注解搞定字段非空」，到「分组、级联分组转换、动态分组、分组顺序」，再到「自定义注解 / 复合注解 / 依赖 Spring Bean 的校验器」，最后是「不走注解的编程式校验」。

<ul>
<li><b>标准 + 实现</b>：校验规则来自 <b>Bean Validation 2.0（JSR-380，<code>javax.validation</code> 包）</b>，运行时实现是 Boot 自带的 <b>Hibernate Validator 6.2.5</b>；<code>org.hibernate.validator.constraints.*</code> 里的注解是它的扩展。</li>
<li><b>声明式为主</b>：绝大多数场景只需在 DTO 字段上加注解 + 在接口参数上加 <code>@Valid</code>，校验失败由 <a href="../00-springboot-common/README.md">00 模块</a>的全局异常处理统一转成 <code>400 + SYS0004</code>，不用写 <code>if (x == null)</code>。</li>
<li><b>零配置</b>：本模块 <code>application.yaml</code> 只有一行 <code>server.port: 8085</code>，校验能力完全由依赖 + 注解驱动。</li>
</ul>

## 1. 学习目标

掌握 Spring Boot 中参数校验的声明式用法与触发机制：知道常见注解各自管什么、<code>@Valid</code> 和 <code>@Validated</code> 的区别、嵌套对象和集合元素怎么校验、什么场景要自定义注解或复合注解、分组（普通分组 / 级联分组转换 / 动态分组 / 分组顺序）分别解决什么问题，以及不通过注解时怎么用 <code>Validator</code> 手动校验；同时能看懂校验失败后异常是怎么一路变成 <code>400 + Result</code> 的。

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
<td>跨字段校验（Getter 上加 <code>@AssertTrue</code>）</td>
<td>在 <code>getXxx()</code> 上写约束，把「密码 = 确认密码」「开始日期 ≤ 结束日期」这类规则变成一个布尔属性</td>
<td><code>POST /valid/assert0</code></td>
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
<td>分组校验</td>
<td><code>groups</code> 属性 + <code>@Validated(CreateGroup.class)</code>：同一个 DTO 在新增 / 更新场景下规则不同</td>
<td><code>POST /valid/group/{create,update,default}</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>级联分组转换</td>
<td><code>@ConvertGroup(from = 外部分组, to = 内部分组)</code>：让嵌套对象不沿用外层分组</td>
<td><code>POST /valid/convertGroup</code></td>
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
<td>分组顺序</td>
<td><code>@GroupSequence</code> 定义先校验「必填」再校验「格式」，前一组不通过就不跑后一组</td>
<td><code>POST /valid/orderedGroup</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>Hibernate Validator 扩展注解</td>
<td><code>@Length</code> / <code>@Range</code> / <code>@URL</code> / <code>@CreditCardNumber</code> / <code>@ISBN</code> / <code>@ScriptAssert</code></td>
<td><code>POST /valid/hibernate</code></td>
</tr>
<tr>
<td>✅</td>
<td>—</td>
<td>四种「触发校验」的写法</td>
<td><code>@RequestBody + @Valid</code>、<code>@RequestBody + @Validated</code>、<code>@RequestParam</code> 上的约束、<code>@PathVariable</code> 上的约束（后两者依赖类级 <code>@Validated</code>）</td>
<td><code>/valid/trigger/*</code></td>
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
<td>现在只把字段错误拼成一个字符串；可以返回 <code>Map&lt;字段, 消息列表&gt;</code> 让前端逐字段标红</td>
<td>—</td>
</tr>
</table>

### 2.2 校验是怎么被触发的（集成链路）

<ol>
<li><b>请求进来</b>：<code>DispatcherServlet</code> 找到 Handler 方法，交给参数解析器。<code>@RequestBody</code> 由 <code>RequestResponseBodyMethodProcessor</code> 处理，表单/查询参数绑定对象由 <code>ModelAttributeMethodProcessor</code> 处理。</li>
<li><b>反序列化 + 绑定</b>：Jackson 把 JSON 转成 DTO；参数上带 <code>@Valid</code>（或 <code>@Validated</code>、或参数本身是 <code>@RequestBody</code> 且被 <code>@Valid</code> 标注）时，解析器会用 <code>WebDataBinder</code> 触发校验。</li>
<li><b>真正执行校验</b>：<code>WebDataBinder</code> 持有 <code>Validator</code>（Boot 自动配置的 <code>LocalValidatorFactoryBean</code>），它把工作交给 Hibernate Validator 的 <code>ValidatorFactory</code>；字段上的注解、类级别注解、<code>groups</code> 在这一步被解释。</li>
<li><b>失败 → 抛异常</b>：<code>@RequestBody</code> 校验失败抛 <code>MethodArgumentNotValidException</code>；表单绑定失败抛 <code>BindException</code>。</li>
<li><b>兜底转换</b>：<a href="../00-springboot-common/README.md">00 模块</a>的 <code>GlobalExceptionHandler</code> 把前两者都转成 <code>400 + Result(SYS0004, "参数校验失败: 字段: 提示; ...")</code>。</li>
<li><b>另一条链路（单参数 / 类级 <code>@Validated</code>）</b>：Controller 类上标了 <code>@Validated</code> 时，Spring 用 <code>MethodValidationPostProcessor</code> 生成代理，方法调用前由 <code>ExecutableValidator</code> 校验参数，失败抛 <code>ConstraintViolationException</code>，同样由 <code>GlobalExceptionHandler</code> 转成 <code>400 + SYS0004</code>（路径会带方法名，见 §4.3.2）。</li>
</ol>

## 3. 依赖与配置

### 3.1 依赖

本模块只有**一个**直接依赖：

```xml
<dependency>
    <groupId>com.hyao.common</groupId>
    <artifactId>00-springboot-common</artifactId>
</dependency>
```

<ul>
<li><code>00-springboot-common</code> 自己声明了 <code>spring-boot-starter-validation</code>（Bean Validation API + Hibernate Validator），所以 05 能直接用 <code>javax.validation.constraints.*</code> 与 <code>org.hibernate.validator.constraints.*</code>；</li>
<li><code>spring-boot-starter-web</code>、Jackson、Lombok、<code>commons-lang3</code> 等来自父 POM 的公共依赖（所有子模块自动继承），所以本模块 POM 里看不到它们；</li>
<li>版本由父 POM 的 <code>spring-boot-dependencies</code> BOM 统一管理（Boot 2.7.18 → Hibernate Validator 6.2.5.Final），子模块不写 <code>&lt;version&gt;</code>。</li>
</ul>

### 3.2 配置

校验本身**没有专属配置项**——注解里已经写全了规则。本模块的 <code>application.yaml</code> 只有端口：

```yaml
server:
  port: 8085
```

和「校验结果长什么样」相关的常用配置如下（都在父 POM / 00 模块的默认值下工作，本模块没有额外打开）：

<table>
<tr>
<th>配置项</th>
<th>作用</th>
<th>本模块取值</th>
</tr>
<tr>
<td><code>server.port</code></td>
<td>端口</td>
<td><code>8085</code>（01/02/03 分别是 8081/8082/8083）</td>
</tr>
<tr>
<td><code>spring.jackson.time-zone</code> / <code>spring.jackson.date-format</code></td>
<td>JSON 里日期时间字段的序列化格式；DTO 上也可以用 <code>@JsonFormat</code> 单独指定（本模块 <code>AssertDTO</code>、<code>HibernateDTO</code> 用的就是注解）</td>
<td>未配置，用 Jackson 默认</td>
</tr>
<tr>
<td><code>spring.jackson.deserialization.fail-on-unknown-properties</code></td>
<td>请求体里多传了 DTO 没有的字段时是否报错</td>
<td>未配置（默认 <code>false</code>：忽略多余字段）</td>
</tr>
<tr>
<td><code>logging.level.org.hibernate.validator</code></td>
<td>看校验器初始化/约束解析的调试日志</td>
<td>未配置</td>
</tr>
<tr>
<td><code>server.error.path</code></td>
<td>容器错误页路径，00 的 <code>CommonErrorController</code> 按它兜底未匹配的 4xx</td>
<td>未配置（默认 <code>/error</code>）</td>
</tr>
</table>

### 3.3 校验失败的响应约定

校验失败统一走 00 模块的全局异常处理，**HTTP 状态码和响应体 <code>code</code> 是两套语义**：

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
<td><code>参数校验失败: id: 新增时 id 必须为空</code>（多字段用 <code>; </code> 拼接）</td>
</tr>
<tr>
<td>表单 / 查询参数绑定对象后校验失败</td>
<td><code>BindException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td>同上</td>
</tr>
<tr>
<td>类级 <code>@Validated</code> 下的单参数校验失败</td>
<td><code>ConstraintViolationException</code></td>
<td><code>400</code></td>
<td><code>SYS0004</code></td>
<td><code>参数校验失败: byParam.id: 不能超过 100</code>（路径带方法名）</td>
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
<td><code>参数校验失败</code>（业务异常，细节只在日志里）</td>
</tr>
<tr>
<td>其它未预期异常（如脚本引擎缺失）</td>
<td><code>Exception</code></td>
<td><code>200</code></td>
<td><code>SYS0001</code></td>
<td><code>系统内部异常</code></td>
</tr>
</table>

## 4. 核心实现

### 4.1 启动配置：为什么「加个依赖 + 加个注解」就生效

<ul>
<li><b>启动类</b>：<code>ValidationApplication</code> 只有 <code>@SpringBootApplication</code>，没有 <code>@EnableWebMvc</code>、没有 <code>@EnableMethodValidation</code> 之类的东西。</li>
<li><b>校验器从哪来</b>：Boot 的 <code>ValidationAutoConfiguration</code> 检测到 classpath 上有 Hibernate Validator，就注册一个 <code>LocalValidatorFactoryBean</code>（同时实现 Spring 的 <code>Validator</code> 和 Bean Validation 的 <code>ValidatorFactory</code>/<code>Validator</code>），把它作为 <code>@Primary</code> 的 <code>Validator</code> Bean——所以 <code>ProgrammaticService</code> 里可以直接构造器注入 <code>javax.validation.Validator</code>。</li>
<li><b>参数校验怎么接上</b>：<code>WebMvcAutoConfiguration</code> 会把这个 <code>Validator</code> 装配进 <code>WebDataBinder</code>，<code>@Valid</code> 参数因此有校验器可用。</li>
<li><b>单参数校验怎么接上</b>：classpath 上有 Bean Validation 时，Boot 注册 <code>MethodValidationPostProcessor</code>；<code>@Validated</code> 标在 <code>TriggerController</code> / <code>OrderedGroupController</code> 类上，Spring 为它们生成 AOP 代理，<code>@RequestParam</code>、<code>@PathVariable</code> 上的约束才生效（不标就完全不起作用）。</li>
<li><b>异常收口</b>：00 模块的 <code>CommonWebAutoConfiguration</code> 通过 starter 的 <code>AutoConfiguration.imports</code> 注册了 <code>GlobalExceptionHandler</code>，本模块不用写任何 try-catch 就得到结构化错误。</li>
</ul>

### 4.2 具体实现

#### 4.2.1 基本注解：`BasicAnnotatedDTO`

[`BasicAnnotatedDTO`](src/main/java/com/hyao/validation/basic/dto/BasicAnnotatedDTO.java) 把 JSR-380 内置注解几乎列了一遍，每个字段一条注释说明语义：

<ul>
<li><b>空值类</b>：<code>@Null</code>（必须为 null）、<code>@NotNull</code>（不能为 null）、<code>@NotEmpty</code>（不能为 null 且不能为空，对 String/集合/Map/数组都有效）、<code>@NotBlank</code>（只对 CharSequence，且会去掉首尾空格后判断）。</li>
<li><b>长度/大小</b>：<code>@Size(min, max)</code> 对 String、集合、Map、数组都成立。</li>
<li><b>数值</b>：<code>@Min</code>/<code>@Max</code>（long 精度）、<code>@DecimalMin</code>/<code>@DecimalMax</code>（字符串形式，可指定是否包含边界）、正负号类 <code>@Positive</code> / <code>@PositiveOrZero</code> / <code>@Negative</code> / <code>@NegativeOrZero</code>、<code>@Digits(integer, fraction)</code> 限制整数位和小数位。</li>
<li><b>格式</b>：<code>@Email</code>、<code>@Pattern(regexp)</code>。</li>
<li><b>时间</b>：<code>@Past</code> / <code>@PastOrPresent</code> / <code>@Future</code> / <code>@FutureOrPresent</code>（支持 <code>java.util.Date</code>、<code>java.time</code> 类型、<code>Calendar</code>）。</li>
<li><b>布尔</b>：<code>@AssertTrue</code> / <code>@AssertFalse</code> 直接约束字段值。</li>
<li><b>共同规则</b>：除 <code>@NotNull</code> / <code>@NotEmpty</code> / <code>@NotBlank</code> 外，<b>其它注解都把 <code>null</code> 视为合法</b>，所以「必填 + 格式」要两个注解一起写。</li>
</ul>

#### 4.2.2 跨字段校验：在 Getter 上写 `@AssertTrue`（`AssertDTO`）

字段之间有关联（密码两次输入、日期区间）时，可以写一个符合 JavaBean getter 规范的方法并在上面标 <code>@AssertTrue</code>：

```java
@AssertTrue(message = "密码不一致")
@JsonIgnore
private Boolean getPasswordMatch() {
    if (password == null || confirmPassword == null) {
        return true;   // 空值交给 @NotBlank 去报，避免重复报错
    }
    return password.equals(confirmPassword);
}
```

<ul>
<li>方法必须<b>无参</b>、<b>返回 <code>boolean</code>/<code>Boolean</code></b>、<b>命名符合 <code>getXxx</code></b>，否则不会被当成属性校验；</li>
<li>校验失败时字段名就是属性名 <code>passwordMatch</code>（实测返回 <code>passwordMatch: 密码不一致</code>）；</li>
<li>加 <code>@JsonIgnore</code> 是为了不让这个「虚拟属性」出现在参与序列化的属性里。</li>
</ul>

#### 4.2.3 嵌套对象与容器元素：`BaseDTO` / `NestedDTO`

<ul>
<li><b>嵌套对象</b>：字段上加 <code>@Valid</code> 才会级联校验内部约束；只写 <code>@NotNull</code> 只保证对象本身不为 null。</li>
<li><b>对象集合</b>：<code>@Valid List&lt;NestedDTO&gt;</code> 或 <code>List&lt;@Valid NestedDTO&gt;</code> 两种写法等价（校验结果里的路径都是 <code>list1[0].id</code>）。</li>
<li><b>元素本身是简单值</b>：<code>List&lt;@NotBlank String&gt;</code>，错误路径是 <code>list3[1]</code>。</li>
<li><b>Map 的键和值</b>：<code>Map&lt;@NotBlank String, @NotBlank String&gt;</code>，错误路径是 <code>map[id]</code>（键合法、值不合法）。</li>
</ul>

#### 4.2.4 自定义单字段注解：`@Phone` + `PhoneValidator`

三步：

<ol>
<li>写注解：<code>@Target({FIELD, PARAMETER, TYPE_USE})</code>、<code>@Retention(RUNTIME)</code>、<code>@Constraint(validatedBy = PhoneValidator.class)</code>，并声明 <code>message()</code> / <code>groups()</code> / <code>payload()</code> 三个必备属性；</li>
<li>写校验器：<code>implements ConstraintValidator&lt;Phone, String&gt;</code>，在 <code>isValid</code> 里返回是否合法；<b><code>null</code> 一般返回 <code>true</code></b>（空值交给 <code>@NotBlank</code>）；</li>
<li>在字段上使用：<code>@Phone</code>，需要必填就再叠一个 <code>@NotBlank</code>。</li>
</ol>

#### 4.2.5 类级别注解 + 把错误挂到具体字段：`@PasswordMatch`

<code>@Target(TYPE)</code> 的约束校验的是整个对象（属于「类级别约束」），默认错误路径是「类」，前端拿不到是哪个字段错。`PasswordMatchValidator` 演示了手动改路径：

```java
context.disableDefaultConstraintViolation();          // 不关掉会出现两条错误
context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode("confirmPassword")            // 把错误挂到 confirmPassword 上
        .addConstraintViolation();
return false;
```

实测返回 <code>confirmPassword: 两次输入的密码不一致</code>。

#### 4.2.6 校验器里注入 Spring Bean：`@UsernameUnique`

`UsernameUniqueValidator` 通过<b>构造器</b>注入 <code>UserRepository</code>，在 <code>isValid</code> 里查「用户名是否已存在」：

<ul>
<li>校验器实例由 Spring 的 <code>ConstraintValidatorFactory</code> 创建（内部走 <code>AutowireCapableBeanFactory.createBean</code>），所以<b>构造器注入 / 字段注入都能拿到容器里的 Bean</b>；</li>
<li>本模块约定校验器上<b>不加 <code>@Component</code></b>——让 HV 管理生命周期即可，加了反而容易变成「被容器和 HV 各持有一份」；</li>
<li><code>UserRepository</code> 是模拟实现（`"hyao"` 视为已存在），不连数据库。</li>
</ul>

#### 4.2.7 复合注解：`@ValidUsername`

把多条内置约束打包成一个语义化注解：

```java
@NotBlank
@Size(min = 2, max = 10)
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
@ReportAsSingleViolation                     // 多条违规只报一条（用本注解的 message）
@ConstraintComposition(CompositionType.AND)  // 所有约束都满足才算通过
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})                // 复合注解不需要自己写校验器
public @interface ValidUsername { ... }
```

#### 4.2.8 分组校验：`GroupDTO` + `CreateGroup` / `UpdateGroup`

同一个 DTO 在「新增」和「更新」时规则不同：新增要求 <code>id</code> 为空，更新要求 <code>id</code> 不为空。

<ul>
<li>约束上用 <code>groups</code> 指定它属于哪些分组：<code>@Null(groups = CreateGroup.class)</code>、<code>@NotNull(groups = UpdateGroup.class)</code>；</li>
<li>接口参数上指定本次校验用哪个分组：<code>@Validated(CreateGroup.class)</code>；</li>
<li>分组接口是<b>空标记接口</b>，不写 <code>groups</code> 的约束属于默认分组 <code>Default</code>；</li>
<li>本模块的 <code>GroupDTO.remark</code> <b>故意不写 <code>groups</code></b>，所以它只在 <code>/valid/group/default</code>（<code>@Validated</code> 不带参数）时校验——用来对比「分组校验不会顺带校验默认分组」。</li>
</ul>

#### 4.2.9 级联分组转换：`ConvertGroupDTO` + `@ConvertGroup`

嵌套对象默认<b>沿用外层分组</b>。如果外层用 <code>OuterCreateGroup</code>、内层 <code>GroupDTO</code> 的约束却挂在 <code>CreateGroup</code> 上，就必须在嵌套字段上做转换：

```java
@Valid
@ConvertGroup(from = OuterCreateGroup.class, to = CreateGroup.class)
private GroupDTO dto;
```

<b>重要坑</b>：<code>@ConvertGroup</code> 在 Bean Validation 2.0 里是 <code>@Repeatable</code> 的，javac 会把同一个字段上的多个 <code>@ConvertGroup</code> 合成一个 <code>@ConvertGroup.List</code> 容器注解，而 <b>Hibernate Validator 6.2.5 只识别直接标注的 <code>@ConvertGroup</code></b>，容器注解被忽略——表现为「转换静默失效、内层完全没校验」，而不是报错。所以一个字段只写一个 <code>@ConvertGroup</code>；多个场景就拆成多个 DTO，或让控制器直接请求内层分组。

#### 4.2.10 动态分组（条件必填）：`DynamicDTO` + `@GroupSequenceProvider`

「类型选 OTHER 时说明字段必填」这种<b>规则依赖对象自身字段值</b>的需求，用 <code>@GroupSequenceProvider</code>：

```java
@GroupSequenceProvider(DynamicGroupProvider.class)
public class DynamicDTO {
    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "说明不能为空", groups = OtherGroup.class)
    private String description;
}
```

<ul>
<li><code>DynamicGroupProvider</code> 实现 <code>DefaultGroupSequenceProvider&lt;DynamicDTO&gt;</code>，在 <code>getValidationGroups(dto)</code> 里决定本次的分组序列；</li>
<li><b>必须把 DTO 自身（<code>DynamicDTO.class</code>）加进序列</b>，它代表 Default 分组；</li>
<li>默认分组触发时顺序是 <code>[DynamicDTO, OtherGroup]</code>：先校验 <code>type</code>，<code>type == "OTHER"</code> 时再追加 <code>OtherGroup</code> 去校验 <code>description</code>。</li>
</ul>

#### 4.2.11 分组顺序：`@GroupSequence` + 类级 `@Validated`

<code>OrderedGroupDTO</code> 里，必填类约束属于 <code>NotBlankGroup</code>、格式类约束属于 <code>ValidGroup</code>，用 <code>@GroupSequence</code> 规定顺序：

```java
@GroupSequence({NotBlankGroup.class, ValidGroup.class})
public interface OrderedGroup { }
```

<ul>
<li>控制器类上标 <code>@Validated(OrderedGroup.class)</code>（<b>标在类上</b>），方法参数仍用 <code>@Valid</code>；</li>
<li>效果：<b>第一组有违规就停</b>，第二组的违规不会返回——所以「必填没填」时看不到「格式不对」的错误，这是设计意图，不是 bug；</li>
<li>副作用：类级 <code>@Validated</code> 会让校验走方法级校验链路，错误路径变成 <code>validation.orderedGroupDTO.xxx</code>（见 §4.3.2）。</li>
</ul>

#### 4.2.12 Hibernate Validator 扩展注解：`HibernateDTO`

<ul>
<li><code>@Length(min, max)</code>：等价 <code>@Size</code>，但语义更明确（只对字符串）；</li>
<li><code>@Range(min, max)</code>：等价 <code>@Min</code> + <code>@Max</code>；</li>
<li><code>@URL(protocol, host)</code>：基于正则的 URL 校验，可限制协议与主机；</li>
<li><code>@CreditCardNumber</code>：Luhn 算法，能挡掉随便敲的数字串；</li>
<li><code>@ISBN(type = ANY)</code>：书号校验；</li>
<li><code>@ScriptAssert(lang, script, reportOn)</code>：类级别、用脚本表达式做跨字段校验，<code>reportOn</code> 指定把错误挂到哪个字段。它依赖 JSR-223 脚本引擎，官方已不推荐（见 §4.4 第 7 条）。</li>
</ul>

#### 4.2.13 四种触发方式 + 编程式校验：`TriggerController` / `ProgrammaticService`

<table>
<tr>
<th>方式</th>
<th>写法</th>
<th>是否依赖类级 <code>@Validated</code></th>
<th>失败异常 / 路径</th>
</tr>
<tr>
<td>1. <code>@RequestBody</code> + <code>@Valid</code></td>
<td><code>byBody1(@Valid @RequestBody TriggerDTO dto)</code></td>
<td>否</td>
<td><code>MethodArgumentNotValidException</code>，路径 <code>id</code></td>
</tr>
<tr>
<td>2. <code>@RequestBody</code> + <code>@Validated</code></td>
<td><code>byBody2(@Validated @RequestBody TriggerDTO dto)</code></td>
<td>否</td>
<td>同上</td>
</tr>
<tr>
<td>3. <code>@RequestParam</code> 上的约束</td>
<td><code>byParam(@RequestParam("id") @Max(100) Integer id)</code></td>
<td><b>是</b></td>
<td><code>ConstraintViolationException</code>，路径 <code>byParam.id</code></td>
</tr>
<tr>
<td>4. <code>@PathVariable</code> 上的约束</td>
<td><code>byPath(@PathVariable("id") @Max(100) Integer id)</code></td>
<td><b>是</b></td>
<td><code>ConstraintViolationException</code>，路径 <code>byPath.id</code></td>
</tr>
<tr>
<td>5. 编程式</td>
<td>注入 <code>javax.validation.Validator</code>，<code>validator.validate(dto)</code></td>
<td>否</td>
<td>自己抛 <code>BizException(SysErrorCode.PARAM_INVALID)</code></td>
</tr>
</table>

编程式校验适合「校验发生在 Service 内部、对象不是从 HTTP 请求直接来的」场景：<code>ProgrammaticService</code> 把违规消息用 <code>"; "</code> 拼起来写日志，然后抛 <code>BizException</code>——注意这属于<b>业务异常</b>，HTTP 状态码是 <code>200</code>，响应体里的 message 是错误码的固定文案（字段明细只在日志里，见 §4.4 第 6 条）。

### 4.3 依赖之间怎么组合关联的

#### 4.3.1 三方各管一段

<table>
<tr>
<th>层</th>
<th>由谁提供</th>
<th>负责什么</th>
</tr>
<tr>
<td>约束注解（标准）</td>
<td><code>jakarta.validation:jakarta.validation-api</code>（包名仍是 <code>javax.validation</code>）</td>
<td>定义 <code>@NotNull</code>、<code>@Size</code>、<code>groups</code>、<code>@Constraint</code> 等契约</td>
</tr>
<tr>
<td>校验实现</td>
<td><code>org.hibernate.validator:hibernate-validator</code>（Boot 2.7.18 带 6.2.5.Final）</td>
<td>解释注解、执行 <code>ConstraintValidator</code>、维护 <code>ValidatorFactory</code>/<code>Validator</code>，并提供 <code>org.hibernate.validator.constraints.*</code> 扩展注解和 <code>@GroupSequenceProvider</code> 等 SPI 实现</td>
</tr>
<tr>
<td>Spring 适配</td>
<td><code>spring-boot-starter-validation</code> → <code>LocalValidatorFactoryBean</code> + <code>MethodValidationPostProcessor</code></td>
<td>把 HV 的 <code>Validator</code> 暴露成 Spring Bean、接进 <code>WebDataBinder</code>、给 <code>@Validated</code> 类生成校验代理、把违规转成 Spring 的异常</td>
</tr>
<tr>
<td>错误响应</td>
<td><a href="../00-springboot-common/README.md">00 模块</a>的 <code>GlobalExceptionHandler</code></td>
<td>把 <code>MethodArgumentNotValidException</code> / <code>ConstraintViolationException</code> 转成 <code>400 + Result(SYS0004)</code></td>
</tr>
</table>

#### 4.3.2 两条链路的错误路径为什么不一样

<ul>
<li><b><code>@Valid @RequestBody</code> / <code>@Validated @RequestBody</code></b>：由参数解析器把 JSON 反序列化后交给 <code>WebDataBinder</code>，错误是 Spring 的 <code>FieldError</code>，路径就是字段名（嵌套时是 <code>dto.id</code>、<code>list1[0].id</code>、<code>map[id]</code>）；</li>
<li><b>类级 <code>@Validated</code></b>：走 <code>MethodValidationPostProcessor</code> 生成的代理，参数整体交给 HV 的 <code>ExecutableValidator</code>，路径是 <b><code>方法名.参数名.字段</code></b>，例如 <code>byParam.id</code>、<code>validation.orderedGroupDTO.name</code>。</li>
<li>两条链路最终都汇到 00 的 <code>GlobalExceptionHandler</code>，所以响应结构一致，只有 message 里的字段路径前缀不同。</li>
</ul>

### 4.4 容易踩坑的点

<ol>
<li><b>基本类型 + <code>@AssertTrue</code> 会「默认失败」</b>：<code>private boolean fieldBoolean</code> 上的 <code>@AssertTrue</code> 意味着「必须为 true」，请求里不传这个字段时 Jackson 保持默认值 <code>false</code> → 直接违规。实测 <code>POST /valid/basic {}</code> 返回 6 条错误，其中就有 <code>fieldBoolean: 字段值必须为 true</code>。想「不传也合法」请用包装类型 <code>Boolean</code>。</li>
<li><b>一个字段写两个 <code>@ConvertGroup</code> 会静默失效</b>：javac 合成 <code>@ConvertGroup.List</code>，HV 6.2.5 不认，级联分组转换完全不生效（见 §4.2.9）。</li>
<li><b><code>@GroupSequence</code> 是「短路」的</b>：第一组不通过，后面的组不会执行，所以别指望同时看到「必填」和「格式」两类错误（见 §4.2.11）。</li>
<li><b>分组校验不会顺带校验默认分组</b>：<code>@Validated(CreateGroup.class)</code> 只跑 <code>CreateGroup</code> 里的约束；<code>remark</code> 这种不写 <code>groups</code> 的字段必须额外指定分组，或在参数上不写分组用 <code>@Validated</code>。</li>
<li><b>类级 <code>@Validated</code> 会给错误路径加方法名前缀</b>：<code>byParam.id</code>、<code>validation.orderedGroupDTO.id</code>，前端按字段名匹配时要考虑这个差异（见 §4.3.2）。</li>
<li><b>编程式校验的 message 不含字段明细</b>：<code>throw new BizException(SysErrorCode.PARAM_INVALID)</code> 只带错误码的固定文案「参数校验失败」；字段明细被写进日志。想让前端看到明细，得自己拼 message（或换一个错误码）。</li>
<li><b><code>@ScriptAssert</code> 依赖 JSR-223 脚本引擎</b>：JDK 8 自带 Nashorn，实测正常；<b>JDK 15+ 已移除 Nashorn</b>，实测（JDK 21 运行）调用 <code>/valid/hibernate</code> 会抛 <code>ConstraintDeclarationException: HV000170: No JSR-223 scripting engine could be bootstrapped for language "javascript"</code>，被兜底成 <code>200 + SYS0001 系统内部异常</code>。要兼容高版本 JDK 需引入 <code>org.openjdk.nashorn:nashorn-core</code>，更推荐改成自定义校验器。</li>
<li><b><code>message</code> 文案和实际规则可能对不上</b>：<code>BasicAnnotatedDTO</code> 里 <code>@Min(value = 1, message = "字段值不能小于 0")</code>、<code>@Max(value = 10, message = "字段值不能大于 150")</code>，提示语和阈值不一致（示例代码里的复制遗留），排错时以注解的 value 为准。</li>
<li><b>类级别约束需要手动挂字段</b>：不加 <code>disableDefaultConstraintViolation()</code> 会出现「类级一条 + 字段一条」重复报错；不加 <code>addPropertyNode()</code> 则前端无法定位字段（见 §4.2.5）。</li>
<li><b>校验器里的 <code>null</code> 判断</b>：自定义校验器对 <code>null</code> 一般应返回 <code>true</code>，必填交给 <code>@NotBlank</code>/<code>@NotNull</code>，否则会出现「两个错误码描述同一件事」。</li>
</ol>

## 5. 快速验证

启动 <a href="src/main/java/com/hyao/validation/ValidationApplication.java"><code>ValidationApplication</code></a>（端口 <b>8085</b>，无需数据库），然后按下面的请求实测。

```bash
# 1. 基本注解：空对象（注意字段顺序不固定，message 里用 ; 拼接）
curl -s -X POST http://localhost:8085/valid/basic -H 'Content-Type: application/json' -d '{}'

# 2. 跨字段 @AssertTrue：密码不一致 + 日期倒置
curl -s -X POST http://localhost:8085/valid/assert0 -H 'Content-Type: application/json' \
  -d '{"begin":"2022-01-01","end":"2021-01-03","password":"123456","confirmPassword":"1234567"}'

# 3. 自定义校验：用户名已存在(hyao) + 手机号格式错 + 两次密码不一致
curl -s -X POST http://localhost:8085/valid/custom -H 'Content-Type: application/json' \
  -d '{"username":"hyao","phone":"1341123","password":"123456","confirmPassword":"12345116"}'

# 4. 分组校验：create 要求 id 为空
curl -s -X POST http://localhost:8085/valid/group/create -H 'Content-Type: application/json' \
  -d '{"id":123,"name":"hyao","remark":""}'

# 5. 级联分组转换
curl -s -X POST http://localhost:8085/valid/convertGroup -H 'Content-Type: application/json' \
  -d '{"dto":{"id":123,"name":"hyao","remark":""}}'

# 6. 动态分组：type=OTHER 时 description 必填
curl -s -X POST http://localhost:8085/valid/dynamicGroup -H 'Content-Type: application/json' \
  -d '{"type":"OTHER","description":""}'

# 7. 分组顺序：两组都不通过时只返回第一组
curl -s -X POST http://localhost:8085/valid/orderedGroup -H 'Content-Type: application/json' \
  -d '{"id":"123","name":"aaacccs","address":"345","phone":"111222333444"}'

# 8. 嵌套/集合/Map
curl -s -X POST http://localhost:8085/valid/nested -H 'Content-Type: application/json' \
  -d '{"nested":{"id":"12345"},"list1":[{"id":"123456"}],"list2":[{"id":"123456"}],"list3":["111",""],"map":{"id":""}}'

# 9. 单参数校验（依赖类级 @Validated）
curl -s 'http://localhost:8085/valid/trigger/param?id=111'

# 10. Hibernate Validator 扩展注解（注意 @Range 的 min=0，r 传 0 是合法的，要用 15 才违规）
curl -s -X POST http://localhost:8085/valid/hibernate -H 'Content-Type: application/json' \
  -d '{"date":"2020-01-01","l":"","r":15,"url":"http://example.com","creditCard":"1234","isbn":"123"}'

# 11. 编程式校验（业务异常，HTTP 200）
curl -s -X POST http://localhost:8085/valid/programmatic -H 'Content-Type: application/json' \
  -d '{"password":"123456","confirmPassword":"1234567"}'

# 12. 路径参数上的约束（依赖类级 @Validated）
curl -s http://localhost:8085/valid/trigger/path/111
```

实测结果（2026-10-24，JDK 8 运行；`400` 的都是 `SYS0004`，`200` 的 `SUC0000` 表示校验通过）：

<table>
<tr>
<th>请求</th>
<th>HTTP</th>
<th>响应体 message（实测原文）</th>
</tr>
<tr>
<td><code>POST /valid/basic</code> 空对象 <code>{}</code></td>
<td><code>400</code></td>
<td><code>参数校验失败: fieldNotBlank: 字符串不能为 null / 空字符串 / 空格; fieldNotEmptyMap: 映射不能为空; fieldNotEmptyList: 列表不能为空; fieldNotNull: 字段不能为 null; fieldNotEmpty: 字符串不能为 null / 空字符串; fieldBoolean: 字段值必须为 true</code></td>
</tr>
<tr>
<td><code>POST /valid/basic</code> 全字段合法</td>
<td><code>200</code></td>
<td><code>操作成功</code></td>
</tr>
<tr>
<td><code>POST /valid/assert0</code> 密码不一致 + 日期倒置</td>
<td><code>400</code></td>
<td><code>参数校验失败: dateValid: 开始日期不能在结束日期之后; passwordMatch: 密码不一致</code></td>
</tr>
<tr>
<td><code>POST /valid/custom</code> 用户名 hyao + 手机号 1341123 + 密码不一致</td>
<td><code>400</code></td>
<td><code>参数校验失败: phone: 手机号码格式不正确; username: 用户名已存在; confirmPassword: 两次输入的密码不一致</code></td>
</tr>
<tr>
<td><code>POST /valid/custom</code> 全部合法</td>
<td><code>200</code></td>
<td><code>操作成功</code></td>
</tr>
<tr>
<td><code>POST /valid/group/create</code> id=123</td>
<td><code>400</code></td>
<td><code>参数校验失败: id: 新增时 id 必须为空</code></td>
</tr>
<tr>
<td><code>POST /valid/group/update</code> 缺 id</td>
<td><code>400</code></td>
<td><code>参数校验失败: id: 更新时 id 不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/group/default</code> remark 为空</td>
<td><code>400</code></td>
<td><code>参数校验失败: remark: 备注不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/convertGroup</code> dto.id=123</td>
<td><code>400</code></td>
<td><code>参数校验失败: dto.id: 新增时 id 必须为空</code></td>
</tr>
<tr>
<td><code>POST /valid/dynamicGroup</code> type=OTHER + description 空</td>
<td><code>400</code></td>
<td><code>参数校验失败: description: 说明不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/dynamicGroup</code> type=1 + description 空</td>
<td><code>200</code></td>
<td><code>操作成功</code></td>
</tr>
<tr>
<td><code>POST /valid/orderedGroup</code> 两组都不通过</td>
<td><code>400</code></td>
<td><code>参数校验失败: validation.orderedGroupDTO.phone: 手机号长度不能超过 11; validation.orderedGroupDTO.name: 名称长度不能超过 6</code></td>
</tr>
<tr>
<td><code>POST /valid/orderedGroup</code> 空对象 <code>{}</code></td>
<td><code>400</code></td>
<td><code>参数校验失败: validation.orderedGroupDTO.address: 地址不能为空; validation.orderedGroupDTO.id: ID不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/hibernate</code> 全字段非法（<code>l:""</code>、<code>r:15</code>、<code>url:http://example.com</code>、<code>date:2020-01-01</code>、<code>creditCard:"1234"</code>、<code>isbn:"123"</code>，JDK 8 运行）</td>
<td><code>400</code></td>
<td><code>参数校验失败: l: 长度必须在 1 到 10 之间; url: URL 必须是 https 协议，主机是 example.com; date: 日期必须大于当前时间; isbn: 无效 ISBN; creditCard: 信用卡号必须符合格式; r: 数值必须在 1 到 10 之间</code></td>
</tr>
<tr>
<td><code>POST /valid/hibernate</code> 全字段合法（<code>l:"abc"</code>、<code>r:5</code>、<code>url:https://example.com/a</code>、<code>date:2099-01-01</code>、<code>creditCard:"4111111111111111"</code>、<code>isbn:"978-3-16-148410-0"</code>）</td>
<td><code>200</code></td>
<td><code>操作成功</code></td>
</tr>
<tr>
<td><code>POST /valid/hibernate</code> 同一报文在 JDK 21 下运行</td>
<td><code>200</code></td>
<td><code>系统内部异常</code>（<code>SYS0001</code>，原因见 §4.4 第 7 条）</td>
</tr>
<tr>
<td><code>POST /valid/nested</code> 示例报文</td>
<td><code>400</code></td>
<td><code>参数校验失败: map[id]: 值不能为空; list2[0].id: 长度不能超过 4; list1[0].id: 长度不能超过 4; nested.id: 长度不能超过 4; list3[1]: 集合中的元素不能为空</code></td>
</tr>
<tr>
<td><code>POST /valid/programmatic</code> 密码不一致</td>
<td><code>200</code></td>
<td><code>参数校验失败</code>（<code>SYS0004</code>，业务异常走 200；字段明细在日志里）</td>
</tr>
<tr>
<td><code>POST /valid/trigger/body1</code> id=123</td>
<td><code>400</code></td>
<td><code>参数校验失败: id: 不能超过 100</code></td>
</tr>
<tr>
<td><code>POST /valid/trigger/body2</code> id=123</td>
<td><code>400</code></td>
<td><code>参数校验失败: id: 不能超过 100</code></td>
</tr>
<tr>
<td><code>GET /valid/trigger/param?id=111</code></td>
<td><code>400</code></td>
<td><code>参数校验失败: byParam.id: 不能超过 100</code></td>
</tr>
<tr>
<td><code>GET /valid/trigger/path/111</code></td>
<td><code>400</code></td>
<td><code>参数校验失败: byPath.id: 不能超过 100</code></td>
</tr>
</table>

> 多字段同时违规时，错误的**顺序不保证稳定**（取决于 `BindingResult` 内部的收集顺序），上表按「错误集合」核对，不要依赖顺序。

## 6. 常见问题与踩坑

<table>
<tr>
<th>现象</th>
<th>原因</th>
<th>处理</th>
</tr>
<tr>
<td>接口收到非法参数仍然返回 <code>SUC0000</code></td>
<td>参数上没有 <code>@Valid</code>/<code>@Validated</code>；或嵌套对象字段没加 <code>@Valid</code>；或 <code>@RequestParam</code> 上的约束缺类级 <code>@Validated</code></td>
<td>按 §2.2 的链路对照检查：外层 <code>@Valid</code>、嵌套 <code>@Valid</code>、类级 <code>@Validated</code> 三处缺一不可</td>
</tr>
<tr>
<td>返回 <code>400</code> 但 message 是「参数校验失败，请求体解析失败」</td>
<td>JSON 本身不合法，或字段类型对不上（例如 <code>id</code> 传了字符串、日期格式不是 <code>yyyy-MM-dd</code>）</td>
<td>这属于反序列化失败，不是约束校验失败，先修 JSON</td>
</tr>
<tr>
<td><code>/valid/convertGroup</code> 明明 id 有值却不报错</td>
<td>字段上写了两个 <code>@ConvertGroup</code>，被合成 <code>@ConvertGroup.List</code>，HV 6.2.5 忽略</td>
<td>拆成多个 DTO，或让控制器直接请求内层分组（见 §4.2.9）</td>
</tr>
<tr>
<td><code>/valid/orderedGroup</code> 只报「必填」，看不到格式错误</td>
<td><code>@GroupSequence</code> 短路：第一组失败就停</td>
<td>先修好必填项再调一次；或把两组拆成两个接口（见 §4.2.11）</td>
</tr>
<tr>
<td><code>/valid/group/create</code> 传 <code>remark: ""</code> 不报错</td>
<td><code>remark</code> 属于默认分组，<code>@Validated(CreateGroup.class)</code> 不会跑默认分组</td>
<td>用 <code>/valid/group/default</code> 对比；业务里需要一起校验就把分组接口设计成继承 <code>Default</code>，或在该约束上补 <code>groups</code></td>
</tr>
<tr>
<td>自定义校验器里注入的 Bean 是 <code>null</code></td>
<td>校验器被 <code>new</code> 出来、没有经过 Spring 创建；或用了字段注入但校验器是静态/工具类</td>
<td>交给 HV 创建，构造器注入（本模块做法）；不要自己 <code>new</code> 校验器</td>
</tr>
<tr>
<td><code>/valid/hibernate</code> 返回 <code>SYS0001</code></td>
<td>JDK 15+ 没有 Nashorn，<code>@ScriptAssert</code> 初始化失败（<code>HV000170</code>）</td>
<td>用 JDK 8 运行，或改成自定义校验器，或引入 <code>nashorn-core</code>（见 §4.4 第 7 条）</td>
</tr>
<tr>
<td>返回 200 但 <code>code</code> 是 <code>SYS0004</code></td>
<td>编程式校验抛的是 <code>BizException</code>，按本工程约定「业务异常 200 + 业务码」</td>
<td>看 <code>code</code> 而不是 HTTP 状态码（见 §3.3）</td>
</tr>
<tr>
<td>多字段校验时错误顺序每次都不同</td>
<td>Spring 的 <code>BindingResult</code> 里 <code>ObjectError</code> 的顺序不保证稳定</td>
<td>前端按字段名匹配，不要依赖顺序；需要固定顺序就自己排序后再拼接</td>
</tr>
</table>

## 7. 学习笔记

<ul>
<li><b>标准 vs 实现</b>：<code>javax.validation.constraints.*</code> 是规范（JSR-380），Hibernate Validator 是它的实现；<code>org.hibernate.validator.constraints.*</code> 与 <code>@GroupSequenceProvider</code> 等属于实现方扩展，换实现（如 Apache BVal）就不保证可用。</li>
<li><b>默认分组是无处不在的</b>：不写 <code>groups</code> 的约束都属于 <code>Default</code> 分组；<code>@Valid</code> 等价于「按默认分组校验」。理解了这一点，<code>@Validated(groups)</code>、<code>@GroupSequence</code>、<code>@ConvertGroup</code>、<code>@GroupSequenceProvider</code> 就串起来了。</li>
<li><b>校验器应该无状态</b>：HV 会缓存校验器实例，多个请求/线程可能复用同一个实例，所以不要在 <code>ConstraintValidator</code> 的字段里存请求相关的数据。</li>
<li><b>校验只解决「形状」，不解决「业务」</b>：唯一性、库存、状态机这类需要查库/查状态的规则，要么像 <code>@UsernameUnique</code> 那样显式依赖 Repository（注意并发与性能），要么放到 Service 里做（编程式或业务判断）。</li>
<li><b>错误响应是「约定」不是「框架」</b>：本工程能统一返回 <code>400 + SYS0004</code>，靠的是 00 模块的 <code>GlobalExceptionHandler</code>；换个项目要自己写这段收口逻辑。</li>
</ul>
