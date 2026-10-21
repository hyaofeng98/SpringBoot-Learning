package com.hyao.mybatis.model.order;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 订单分页查询响应.
 */
@Data
public class OrderPageVO {

    /** 是否存在下一页 */
    private boolean hasNextPage;

    /** 下一页起始游标 */
    private String nextCursor;

    /** 记录 */
    private List<Map<String, Object>> rows;
}
