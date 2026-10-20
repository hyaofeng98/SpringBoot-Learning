package com.hyao.mybatis.common.id;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于雪花算法的主键生成器.
 */
@Slf4j
public class SnowflakeIdGenerator {
    /** 起始纪元时间戳 2020-01-01 */
    private static final long EPOCH = 1577836800000L;

    /** 机器ID占用位数 */
    private static final long WORKER_ID_BITS = 5L;
    /** 数据中心ID占用位数 */
    private static final long DATACENTER_ID_BITS = 5L;
    /** 序列号占用位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    /** 最大数据中心ID */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    /** 数据中心ID左移位数 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /** 时钟回拨最大容忍毫秒数, 超过则抛异常 */
    private static final long MAX_BACKWARD_MS = 5L;


    private final long datacenterId;
    private final long workerId;

    /** 上一次生成 ID 的毫秒时间戳 */
    private long lastTimestamp = -1L;
    /** 当前毫秒内的序列号 */
    private long sequence = 0L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(String.format("workerId 必须在 [0, %d] 范围内", MAX_WORKER_ID));
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(String.format("datacenterId 必须在 [0, %d] 范围内", MAX_DATACENTER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        log.info("SnowflakeIdGenerator 初始化完成, workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * 生成下一个 ID.
     *
     * @return 唯一 ID
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();
        // 时钟回拨检测
        if (currentTimestamp < lastTimestamp) {
            long backward = lastTimestamp - currentTimestamp;
            if (backward <= MAX_BACKWARD_MS) {
                // 小幅回拨: 等待时钟追上
                log.warn("检测到时钟小幅回拨 {}ms, 等待中...", backward);
                waitUntil(lastTimestamp);
                currentTimestamp = System.currentTimeMillis();
            } else {
                // 大幅回拨: 抛异常, 由上级决定降级策略
                throw new IllegalArgumentException(String.format("时钟回拨过大: 回拨 %dms, 拒绝生成 ID", backward));
            }
        }
        // 同一毫秒内
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK; // 12位
            if (sequence == 0L) {
                // 同毫秒序列用尽, 等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的一毫秒, 序列号归零
            sequence = 0L;
        }
        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 自旋等待直到指定时间戳
     */
    private void waitUntil(long targetTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetTimestamp) {
            Thread.yield();
            timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 阻塞直到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            // 等待下一毫秒
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
