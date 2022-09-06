package com.alibaba.dubbo.demo.consumer.utils;

import org.slf4j.MDC;

public class TraceIdUtils {

    /**
     * 获取当前链路号
     *
     * @return
     */
    public static String get() {
        return MDC.get(TraceIdFactory.getInstance().getKey());
    }

    /**
     * 生成TraceId
     *
     * @return
     */
    public static String generate() {
        return TraceIdFactory.getInstance().generate();
    }

    /**
     * 设置当前的链路号
     *
     * @param traceId
     */
    public static void set(String traceId) {
        MDC.put(TraceIdFactory.getInstance().getKey(), traceId);
    }

}
