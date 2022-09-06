package com.alibaba.dubbo.demo.consumer.spi.consumer;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.demo.consumer.utils.TraceIdUtils;
import com.alibaba.dubbo.rpc.*;

public class ConsumerTraceFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // dubbo 保证此时RpcContext非空，被非dubbo方式调入时直接跳过
        if (null == RpcContext.getContext()) {
            return invoker.invoke(invocation);
        }
        String traceId = RpcContext.getContext().getAttachment("TRACE_ID");

        if (StringUtils.isBlank(traceId)) {
            traceId = TraceIdUtils.get();
            if (StringUtils.isBlank(traceId)) {
                traceId = TraceIdUtils.generate();
            }
            RpcContext.getContext().setAttachment("TRACE_ID", traceId);
        }
        return invoker.invoke(invocation);
    }
}
