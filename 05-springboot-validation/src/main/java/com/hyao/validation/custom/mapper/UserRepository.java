package com.hyao.validation.custom.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 模拟数据访问层, 不连接数据库了.
 */
@Repository
public class UserRepository {

    /**
     * 模拟根据用户名查询数量的逻辑.
     * <p>模拟数据库中存在用户名为 "hyao" 的一条数据.
     */
    public int selectCount(String username) {
        return "hyao".equals(username) ? 1 : 0;
    }

}
