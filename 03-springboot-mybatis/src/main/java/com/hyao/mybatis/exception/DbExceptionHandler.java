package com.hyao.mybatis.exception;

import com.hyao.common.model.Result;
import com.hyao.mybatis.common.errorcode.BizErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 持久层异常处理：把 MyBatis / JDBC 抛出的 {@link DataAccessException} 统一转成带 DBE0001 的 {@link Result}。
 * <p>避免与{@code @ExceptionHandler(Exception.class)}冲突，增加标 {@code @Order(1)}注解。
 * <p>响应里只给固定的「DB操作异常」，SQL 语句、表名、连接串等细节只进日志。
 */
@Slf4j
@RestControllerAdvice
public class DbExceptionHandler {

    /**
     * 处理 {@link DataAccessException} 异常.
     *
     * @param e 异常
     * @return 错误码 DBE0001 的 {@link Result}。
     */
    @Order(1)
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库操作异常", e);
        return Result.fail(BizErrorCode.DB_ERROR.getCode(), BizErrorCode.DB_ERROR.getMessage());
    }
}
