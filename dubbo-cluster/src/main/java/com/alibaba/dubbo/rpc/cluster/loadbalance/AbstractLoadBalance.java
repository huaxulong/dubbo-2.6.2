/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * AbstractLoadBalance
 * 这里封装了一些负载均衡的一些公共逻辑
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * 该过程主要用于保证当服务运行时长小于 10分钟的时候， 对服务进行降权，避免让服务在启动之初 就处于高负载状态。
     * @param uptime
     * @param warmup
     * @param weight
     * @return
     */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        // 计算权重， 下面代码的逻辑上形似于 (uptime / warmup) * weight
        // 随着服务运行时间update 增大， 权重计算值 ww 会慢慢接近配置值 weight
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers == null || invokers.isEmpty()) return null;
        // 如果 invokers 列表仅有一个 Invoker ， 直接返回即可， 无需进行负载均衡
        if (invokers.size() == 1) return invokers.get(0);

        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    /**
     * 服务提供者权重计算
     *
     * @param invoker
     * @param invocation
     * @return
     */
    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 从 url 中获取权重 weight 配置值
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0) {
            // 获取服务提供者的启动时间戳
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                // 计算服务提供者的运行时长
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                // 获取服务预热时间， 默认为10分钟
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                // 如果服务运行时间小于预热时间， 则重新计算服务权重， 即降权
                if (uptime > 0 && uptime < warmup) {
                    // 重新计算服务权重
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }

}
