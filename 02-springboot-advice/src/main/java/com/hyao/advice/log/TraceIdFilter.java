package com.hyao.advice.log;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TraceId生成与清理。使用MDC保存traceId，方便日志跟踪。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 优先取上游传入的traceId，没有则生成
        String traceId = req.getHeader(TraceIdUtils.TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            traceId = TraceIdUtils.generate();
        }
        TraceIdUtils.setTraceId(traceId);
        // 响应头带回traceId，方便前端反馈问题时提供
        resp.setHeader(TraceIdUtils.TRACE_ID, traceId);

        long start = System.currentTimeMillis();
        try {
            log.info("请求开始 {} {}", req.getMethod(), req.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("请求结束 {} {} 耗时={}ms", req.getMethod(), req.getRequestURI(), cost);
            TraceIdUtils.clear();  // 必须清理，避免线程复用串号
        }
    }
}
