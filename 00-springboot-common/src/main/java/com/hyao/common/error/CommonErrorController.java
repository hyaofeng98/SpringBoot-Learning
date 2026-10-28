package com.hyao.common.error;

import com.hyao.common.exception.SysErrorCode;
import com.hyao.common.model.Result;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 把 Spring Boot 默认的 {@code /error} 响应也统一成 {@link Result}。
 *
 * <p>有些错误不会进入 {@code @ControllerAdvice}，而是由容器直接 {@code sendError} 再转到 {@code /error}：
 * <ul>
 *     <li><b>404</b>：URL 没有匹配到任何 Handler（静态资源也不存在时）；</li>
 *     <li>过滤器 / 容器层抛出的异常；</li>
 *     <li>响应已提交后再出错（基本无法补救，只能交给这里）。</li>
 * </ul>
 *
 * <p>注册之后，Spring Boot 自带的 {@code BasicErrorController} 会因为
 * {@code @ConditionalOnMissingBean(ErrorController.class)} 而退让，项目里所有错误响应就都是同一个结构了。
 */
@RestController
public class CommonErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<Result<Void>> error(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        SysErrorCode errorCode = resolveErrorCode(status);
        String path = Objects.toString(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), "");
        String message = (errorCode == SysErrorCode.HANDLER_NOT_FOUND && !path.isEmpty())
                ? errorCode.getMessage() + ": " + path
                : errorCode.getMessage();
        return ResponseEntity.status(status).body(Result.fail(errorCode.getCode(), message));
    }

    private static HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        HttpStatus status = HttpStatus.resolve(Integer.parseInt(statusCode.toString()));
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    private static SysErrorCode resolveErrorCode(HttpStatus status) {
        switch (status) {
            case NOT_FOUND:
                return SysErrorCode.HANDLER_NOT_FOUND;
            case METHOD_NOT_ALLOWED:
                return SysErrorCode.METHOD_NOT_SUPPORTED;
            case BAD_REQUEST:
                return SysErrorCode.PARAM_INVALID;
            default:
                return status.is4xxClientError() ? SysErrorCode.REQUEST_ERROR : SysErrorCode.SYSTEM_ERROR;
        }
    }
}
