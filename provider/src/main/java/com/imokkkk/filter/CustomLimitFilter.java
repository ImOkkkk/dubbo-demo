package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

import cn.hutool.core.util.StrUtil;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机限流
 *
 * @author wyliu
 * @date 2024/8/12 21:12
 * @since 1.0
 */
@Activate(group = PROVIDER)
public class CustomLimitFilter implements Filter {
    // 存储计数资源的Map数据结构，预分配容量64，避免无畏的扩容消耗
    private static final ConcurrentHashMap<String, AtomicInteger> COUNT_MAP =
            new ConcurrentHashMap<>(64);
    // 标识启动QPS限流检测，{@code true}:标识开启限流检测，{@code false 或 null}
    public static final String KEY_QPS_ENABLE = "qps.enable";
    // 每个方法开启的限流检测值
    public static final String KEY_QPS_VALUE = "qps.value";
    // 默认的限流检测值，默认为 30
    public static final long DEFAULT_QPS_VALUE = 30;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取限流资源的结果，acquired 有三种值:
        // true:获取到计数资源;false:计数已满，无法再获取计数资源;null:不需要限流
        Boolean acquired = null;
        try {
            // 尝试是否能获取到限流计数资源
            acquired = tryAcquire(invoker.getUrl(), invocation);
            // 若获取不到计数资源的话，则直接抛出异常即可，告知调用方限流了
            if (acquired != null && !acquired) {
                throw new RuntimeException(
                        "Failed to acquire service "
                                + String.join(
                                        ".",
                                        invoker.getInterface().getName(),
                                        invocation.getMethodName())
                                + " because of overload.");
            }
            return invoker.invoke(invocation);
        } finally {
            // 调用不管是成功还是失败，都是需要进行计数资源释放的
        }
    }

    // 尝试是否能获取到限流计数资源
    private Boolean tryAcquire(URL url, Invocation invocation) {
        // 从方法层面获取 qps.enable 参数值，如果为 true 则表示开启限流控制，否则不需要限流
        String qpsEnableFlag = url.getMethodParameter(invocation.getMethodName(), KEY_QPS_ENABLE);
        if (!StrUtil.equals(Boolean.TRUE.toString(), qpsEnableFlag)) {
            return null;
        }
        // 从方法层面获取 qps.value 限流的标准容量，如果没配置则默认为 30
        long qpsValue =
                url.getMethodParameter(
                        invocation.getMethodName(), KEY_QPS_VALUE, DEFAULT_QPS_VALUE);
        // 服务名加方法名构建Map的Key
        String serviceKey = String.join("_", url.getServiceKey(), invocation.getMethodName());
        // 尝试看看该服务是否有对应的计数对象
        AtomicInteger currentCount =
                COUNT_MAP.computeIfAbsent(serviceKey, k -> new AtomicInteger());
        // 若当前的计数值大于或等于已配置的限流值的话，那么返回 false 表示无法获取计数资源
        if (currentCount.get() >= qpsValue) {
            return false;
        }
        currentCount.incrementAndGet();
        return true;
    }

    private void release(Boolean acquired, URL url, Invocation invocation) {
        // 若不需要限流，或者没有获取到计数资源，都不需要进行计数资源释放
        if (acquired == null || !acquired) {
            return;
        }
        // 释放计数资源
        String serviceKey = String.join("_", url.getServiceKey(), invocation.getMethodName());
        AtomicInteger currentCount = COUNT_MAP.get(serviceKey);
        currentCount.decrementAndGet();
    }
}
