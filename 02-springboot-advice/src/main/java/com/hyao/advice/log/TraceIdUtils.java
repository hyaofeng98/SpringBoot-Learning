package com.hyao.advice.log;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdUtils {
    public static final String TRACE_ID = "traceId";

    private TraceIdUtils(){}

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
