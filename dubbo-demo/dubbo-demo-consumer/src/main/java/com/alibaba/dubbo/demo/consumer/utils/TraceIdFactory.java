package com.alibaba.dubbo.demo.consumer.utils;

import org.apache.commons.lang3.RandomStringUtils;

public class TraceIdFactory {

    /**
     * 随机字符串长度，开放可配置
     */
    private Integer randomLength = 16;

    /**
     * 日志存储中使用的key
     */
    private String key = "TRACE_ID";

    public void setRandomLength(Integer randomLength) {
        this.randomLength = randomLength;
    }

    public String getKey() {
        return key == null ? "TRACE_ID" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private TraceIdFactory() {}

    /**
     * 定义内部枚举类实现单例模式
     */
    private enum TraceIdFactoryEnum {

        // 单例实例
        INSTANCE;

        private TraceIdFactory container;

        TraceIdFactoryEnum() {
            container = new TraceIdFactory();
        }

        public TraceIdFactory getInstance() {
            return container;
        }
    }

    /**
     * 获取单例实例
     *
     * @return
     */
    public  static TraceIdFactory getInstance() {
        return TraceIdFactoryEnum.INSTANCE.getInstance();
    }

    /**
     * 生成TraceId
     *
     * @return
     */
    public String generate() {
        return generate(randomLength);
    }

    /**
     * 生成TraceId
     *
     * @return
     */
    public String generate(Integer randomLength) {
        if (null == randomLength){
            randomLength = 16;
        }
        return RandomStringUtils.randomAlphanumeric(randomLength);

    }

}
