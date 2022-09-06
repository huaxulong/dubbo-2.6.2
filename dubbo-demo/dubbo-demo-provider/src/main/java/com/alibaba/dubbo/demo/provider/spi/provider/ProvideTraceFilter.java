package com.alibaba.dubbo.demo.provider.spi.provider;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.demo.provider.utils.TraceIdUtils;
import com.alibaba.dubbo.rpc.*;

public class ProvideTraceFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // dubbo 保证此时RpcContext非空，被非dubbo方式调入时直接跳过
        if (null == RpcContext.getContext()) {
            return invoker.invoke(invocation);
        }

        String traceId = RpcContext.getContext().getAttachment("TRACE_ID");

        if (StringUtils.isBlank(traceId)) {
            // 远程调用过来的，理论上当前线程上下文是不存在traceId的
            traceId = TraceIdUtils.get();
            if (StringUtils.isBlank(traceId)) {
                traceId = TraceIdUtils.generate();
            }
        }

        TraceIdUtils.set(traceId);
        return invoker.invoke(invocation);
    }
}
