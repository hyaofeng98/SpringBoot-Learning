package com.hyao.mybatis.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 条件分页查询的请求参数。
 *
 * <p>约束注解由 00 模块的 Bean Validation 统一处理：校验失败返回 {@code 400} + {@code SYS0004}
 * （message 形如 {@code 参数校验失败: pageSize: 不能为空}），Controller 只需要在参数上标 {@code @Valid}。
 *
 * <p>注意 {@code pageSize} 是 {@code Integer}：JSON 里传 {@code {"pageSize":"2"}} 时 Jackson 会做字符串到数字的
 * 强制转换（仍然是 2）；传 {@code {"pageSize":"abc"}} 才会解析失败，返回 {@code SYS0004}「请求体解析失败」。
 */
@Data
public class UserPageQuery {

    /** 每页条数，必填，1 ~ 100 */
    @NotNull(message = "不能为空")
    @Min(value = 1, message = "至少为 1")
    @Max(value = 100, message = "最多为 100")
    private Integer pageSize;

    /** 游标：上一页返回的「下一页起始 id」，第一页不传 */
    @Size(max = 16, message = "长度不能超过 16")
    private String nextCursor;

    /** 用户状态：0 禁用 / 1 正常 / 2 删除 */
    private Integer status;

    /** 性别：0 未知 / 1 男 / 2 女 */
    private Integer gender;

    /** 生日起（yyyy-MM-dd） */
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "格式必须为 yyyy-MM-dd")
    private String beginDate;

    /** 生日止（yyyy-MM-dd） */
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "格式必须为 yyyy-MM-dd")
    private String endDate;

    /** 省 */
    private String province;

    /** 市 */
    private String city;
}
