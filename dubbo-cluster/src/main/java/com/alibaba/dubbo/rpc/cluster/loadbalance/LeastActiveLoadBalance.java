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
import com.alibaba.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.Random;

/**
 * LeastActiveLoadBalance
 * 最小活跃数 负载均衡。 活跃调用越小， 表明该服务提供者效率越高， 单位时间内可处理更多的请求。 此时应优先将请求分配给 该服务提供者。
 * 在具体的实现中， 每个服务提供者对应一个 活跃数 active。 初始情况下， 所有服务提供者活跃数均为 0。
 * 每收到一个请求，活跃数加1，完成请求后则将活跃数减1。在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，
 * 此时这样的服务提供者能够优先获取到新的服务请求、这就是最小活跃数负载均衡算法的基本思想。
 * 除了最小活跃数，LeastActiveLoadBalance 在实现上还引入了权重值。所以准确的来说，LeastActiveLoadBalance 是基于加权最小活跃数算法实现的。
 * 举个例子说明一下，在一个服务提供者集群中，有两个性能优异的服务提供者。某一时刻它们的活跃数相同，此时 Dubbo 会根据它们的权重去分配请求，权重越大，获取到新请求的概率就越大。
 * 如果两个服务提供者权重相同，此时随机选择一个即可。
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    private final Random random = new Random();

    /**
     * 遍历 invokers 列表，寻找活跃数最小的 Invoker
     * 如果有多个 Invoker 具有相同的最小活跃数，此时记录下这些 Invoker 在 invokers 集合中的下标，并累加它们的权重，比较它们的权重值是否相等
     * 如果只有一个 Invoker 具有最小的活跃数，此时直接返回该 Invoker 即可
     * 如果有多个 Invoker 具有最小活跃数，且它们的权重不相等，此时处理方式和 RandomLoadBalance 一致
     * 如果有多个 Invoker 具有最小活跃数，但它们的权重相等，此时随机返回一个即可
     * @param invokers
     * @param url
     * @param invocation
     * @return
     * @param <T>
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size(); // Number of invokers
        // 最小活跃数
        int leastActive = -1; // The least active value of all invokers
        //具有相同的 最小活跃数 的服务提供者数量（Invoker）
        int leastCount = 0; // The number of invokers having the same least active value (leastActive)
        // leastIndexs 用于记录具有相同“最小活跃数”的 Invoker 在 invokers 列表中的下标信息
        int[] leastIndexs = new int[length]; // The index of invokers having the same least active value (leastActive)
        int totalWeight = 0; // The sum of weights
        int firstWeight = 0; // Initial value, used for comparision

        // 标识 所有具有最下活跃数的 Invoker 的权重是否 都相等。
        boolean sameWeight = true; // Every invoker has the same weight value?
        // 遍历 invokers 列表
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            // 获取invoker 对应的活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive(); // Active number
            // 获取 权重
            int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT); // Weight
            // 发现更小的 活跃数， 重新开始
            if (leastActive == -1 || active < leastActive) { // Restart, when find a invoker having smaller least active value.
                // 使用当前活跃数 active 更新最小活跃数 leastActive
                leastActive = active; // Record the current least active value
                // 更新 leastCount 为 1
                leastCount = 1; // Reset leastCount, count again based on current leastCount
                // 记录当前下标值到 leastIndexs 中
                leastIndexs[0] = i; // Reset
                totalWeight = weight; // Reset
                firstWeight = weight; // Record the weight the first invoker
                sameWeight = true; // Reset, every invoker has the same weight value?
                // 当前Invoker 的活跃数 active 与 最小活跃数 leastActive 相同 ， 说明所有的invoker 都能参与竞争
            } else if (active == leastActive) { // If current invoker's active value equals with leaseActive, then accumulating.
                // 在 leastIndexs 中记录下当前 Invoker 在 invokers 集合中的下标
                leastIndexs[leastCount++] = i; // Record index number of this invoker
                // 累加权重
                totalWeight += weight; // Add this invoker's weight to totalWeight.
                // If every invoker has the same weight?
                // 检测当前 Invoker 的权重与 firstWeight 是否相等，
                // 不相等则将 sameWeight 置为 false
                if (sameWeight && i > 0
                        && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // assert(leastCount > 0)
        // 当只有一个 Invoker 具有最小活跃数，此时直接返回该 Invoker 即可
        if (leastCount == 1) {
            // If we got exactly one invoker having the least active value, return this invoker directly.
            return invokers.get(leastIndexs[0]);
        }

        // 有多个 Invoker 具有相同的最小活跃数， 但它们之间的权重不同
        if (!sameWeight && totalWeight > 0) {
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            // 随机生成一个 【0，totalWeight】之间的数字
            int offsetWeight = random.nextInt(totalWeight);
            // Return a invoker based on the random value.
            // 循环让随机数 减去 具有最小活跃数的 Invoker 的权重值
            // 当 offset 小于等于0 时， 返回相应的 Invoker
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexs[i];
                // 获取权重值，并让随机数减去权重值 - 这里就是的逻辑跟带权的逻辑一样。
                offsetWeight -= getWeight(invokers.get(leastIndex), invocation);
                if (offsetWeight <= 0)
                    return invokers.get(leastIndex);
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.

        // 如果权重相同 或者权重为0时， 随机返回一个 Invoekr
        return invokers.get(leastIndexs[random.nextInt(leastCount)]);
    }
}
