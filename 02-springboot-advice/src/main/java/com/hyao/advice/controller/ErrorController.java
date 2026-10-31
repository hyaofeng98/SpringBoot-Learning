package com.hyao.advice.controller;

import com.hyao.advice.common.BizErrorCode;
import com.hyao.advice.model.User;
import com.hyao.common.exception.BizException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常返回示例。
 */
@RestController
@RequestMapping("/error")
public class ErrorController {

    /**
     * 演示全局异常处理效果。
     * <pre>
     *     请求示例：
     *     POST http://localhost:8082/error/user
     * </pre>
     *
     * @return 异常演示结果
     */
    @PostMapping("/user")
    public User getUser() {
        // 查询用户...
        throw new BizException(BizErrorCode.QUERY_ERROR, "查询用户为空！");
    }

}
