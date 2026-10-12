package com.hyao.advice.controller;

import com.hyao.advice.common.BizException;
import com.hyao.advice.common.ErrorCode;
import com.hyao.advice.model.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常返回示例。
 */
@RestController
@RequestMapping("/error")
public class ErrorController {

    @PostMapping("/user")
    public User getUser() {
        // 查询用户...
        throw new BizException(ErrorCode.QUERY_ERROR, "查询用户为空！");
    }

}
