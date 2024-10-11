package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 分布式限流
 *
 * @author wyliu
 * @date 2024/9/24 22:19
 * @since 1.0
 */
@Activate(group = PROVIDER)
@Component
public class CustomLimitFilter2 implements Filter {
    private static final ConcurrentHashMap<String, AtomicInteger> COUNT_MAP =
            new ConcurrentHashMap<>(64);
    public static final String KEY_QPS_ENABLE = "qps.enable";
    // qps.type 处理限流的工具，枚举值有:jlimit-JVM限流;rlimit-Redis限流
    public static final String KEY_QPS_TYPE = "qps.type";
    // 处理限流的工具，jlimit-JVM限流
    public static final String VALUE_QPS_TYPE_OF_JLIMIT = "jlimit";
    // 处理限流的工具，rlimit-Redis限流
    public static final String VALUE_QPS_TYPE_OF_RLIMIT = "rlimit";
    public static final String KEY_QPS_VALUE = "qps.value";
    public static final long DEFAULT_QPS_VALUE = 30;
    // 策略分发，通过不同的 qps.type 值来选择不同的限流工具进行获取计数资源处理
    private static final Map<String, BiFunction<URL, Invocation, Boolean>> QPS_TYPE_ACQUIRE_MAP =
            new HashMap<>(4);
    // 策略分发，通过不同的 qps.type 值来选择不同的限流工具进行释放计数资源处理
    private static final Map<String, BiConsumer<URL, Invocation>> QPS_TYPE_RELEASE_MAP =
            new HashMap<>(4);

    private StringRedisTemplate redisTemplate;

    public CustomLimitFilter2() {
        init();
        redisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
    }

    private void init() {
        QPS_TYPE_ACQUIRE_MAP.put(
                VALUE_QPS_TYPE_OF_JLIMIT,
                (url, invocation) -> tryAcquireOfJvmLimit(url, invocation));
        QPS_TYPE_ACQUIRE_MAP.put(
                VALUE_QPS_TYPE_OF_RLIMIT,
                (url, invocation) -> tryAcquireOfRedisLimit(url, invocation));
        QPS_TYPE_RELEASE_MAP.put(
                VALUE_QPS_TYPE_OF_JLIMIT, (url, invocation) -> releaseOfJvmLimit(url, invocation));
        QPS_TYPE_RELEASE_MAP.put(
                VALUE_QPS_TYPE_OF_RLIMIT,
                (url, invocation) -> releaseOfRedisLimit(url, invocation));
    }

    // 通过JVM内存的处理方式，来尝试是否能获取到限流计数资源
    private Boolean tryAcquireOfJvmLimit(URL url, Invocation invocation) {
        long qpsValue =
                url.getMethodParameter(
                        invocation.getMethodName(), KEY_QPS_VALUE, DEFAULT_QPS_VALUE);
        String serviceKey = String.join("-", url.getServiceKey(), invocation.getMethodName());
        AtomicInteger currentCount =
                COUNT_MAP.computeIfAbsent(serviceKey, k -> new AtomicInteger());
        if (currentCount.get() >= qpsValue) {
            return false;
        }
        currentCount.incrementAndGet();
        return true;
    }

    // 通过JVM内存的处理方式，来进行计数资源释放
    private void releaseOfJvmLimit(URL url, Invocation invocation) {
        String serviceKey = String.join("-", url.getServiceKey(), invocation.getMethodName());
        AtomicInteger currentCount = COUNT_MAP.get(serviceKey);
        currentCount.decrementAndGet();
    }

    // 通过Redis的处理方式，来尝试是否能获取到限流计数资源
    private Boolean tryAcquireOfRedisLimit(URL url, Invocation invocation) {
        long qpsValue =
                url.getMethodParameter(
                        invocation.getMethodName(), KEY_QPS_VALUE, DEFAULT_QPS_VALUE);
        String serviceKey = String.join("-", url.getServiceKey(), invocation.getMethodName());
        // 尝试看看该服务在 redis 中当前计数值是多少
        int currentCount =
                NumberUtil.parseInt(
                        ObjectUtil.defaultIfNull(redisTemplate.opsForValue().get(serviceKey), "0"));
        if (currentCount >= qpsValue) {
            return false;
        }
        redisTemplate.opsForValue().increment(serviceKey, 1);
        return true;
    }

    // 通过Redis的处理方式，来进行计数资源释放
    private void releaseOfRedisLimit(URL url, Invocation invocation) {
        String serviceKey = String.join("-", url.getServiceKey(), invocation.getMethodName());
        redisTemplate.opsForValue().decrement(serviceKey, 1);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Boolean acquired = null;
        try {
            acquired = tryAcquire(invoker.getUrl(), invocation);
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
            release(acquired, invoker.getUrl(), invocation);
        }
    }

    private void release(Boolean acquired, URL url, Invocation invocation) {
        if (acquired == null || !acquired) {
            return;
        }
        String qpsTypeVal =
                url.getMethodParameter(
                        invocation.getMethodName(), KEY_QPS_TYPE, VALUE_QPS_TYPE_OF_JLIMIT);
        BiConsumer<URL, Invocation> consumer = QPS_TYPE_RELEASE_MAP.get(qpsTypeVal);
        if (consumer != null) {
            consumer.accept(url, invocation);
        }
    }

    private Boolean tryAcquire(URL url, Invocation invocation) {
        String qpsEnableFlag = url.getMethodParameter(invocation.getMethodName(), KEY_QPS_ENABLE);
        if (!Boolean.TRUE.toString().equals(qpsEnableFlag)) {
            return null;
        }
        String qpsTypeVal =
                url.getMethodParameter(
                        invocation.getMethodName(), KEY_QPS_TYPE, VALUE_QPS_TYPE_OF_JLIMIT);
        BiFunction<URL, Invocation, Boolean> func = QPS_TYPE_ACQUIRE_MAP.get(qpsTypeVal);
        if (func == null) {
            return null;
        }
        return func.apply(url, invocation);
    }
}
