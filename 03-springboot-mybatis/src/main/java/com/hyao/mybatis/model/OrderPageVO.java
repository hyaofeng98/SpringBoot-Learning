package com.hyao.mybatis.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OrderPageVO {

    private boolean hasNextPage;

    private String nextCursor;

    private List<Map<String, Object>> rows;
}
