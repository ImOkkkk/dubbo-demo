package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

import cn.hutool.core.util.StrUtil;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.io.Serializable;

/**
 * @author wyliu
 * @date 2024/10/27 12:29
 * @since 1.0
 */
@Activate(group = {CONSUMER, PROVIDER})
public class CustomCacheFilter implements Filter {
    private CacheFactory cacheFactory;

    public CustomCacheFilter(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (cacheFactory != null
                && StrUtil.equals(
                        invoker.getUrl()
                                .getMethodParameter(invocation.getMethodName(), "cache.enable"),
                        "true")) {
            Cache cache = cacheFactory.getCache(invoker.getUrl(), invocation);
            if (cache != null) {
                // 将方法接口调用的参数值想办法变成一个组合的字符串 key，作为标识缓存结果是否存在的 key
                // nodes样例值情况：
                // 1、id
                // 2、id,name
                // 3、id,name,sex
                // 4、list[0].id,list[1].name,list[2].sex
                String nodes =
                        invoker.getUrl()
                                .getMethodParameter(
                                        invocation.getMethodName(), "cache.unique.nodes");
                String key = null;
                if (StrUtil.isBlank(nodes)) {
                    key = StringUtils.toArgumentString(invocation.getArguments());
                } else {
                    // 该方法的重点逻辑就是根据 nodes 里面的路径，
                    // 从 invocation.getArguments() 取出对应的路径值
                    // 然后将参数值拼接在一起构成key
                    // key = extraNodeValueToArgumentString(nodes);
                }
                Object value = cache.get(key);
                if (value != null) {
                    if (value instanceof ValueWrapper) {
                        return AsyncRpcResult.newDefaultAsyncResult(
                                ((ValueWrapper) value).get(), invocation);
                    } else {
                        return AsyncRpcResult.newDefaultAsyncResult(value, invocation);
                    }
                }
                Result result = invoker.invoke(invocation);
                if (!result.hasException()) {
                    cache.put(key, new ValueWrapper(result.getValue()));
                }
                return result;
            }
        }
        // 如果连 cacheFactory 这个扩展点都没有，说明并不需要缓存，原来该怎么调用就还是接着怎么调用
        return invoker.invoke(invocation);
    }

    static class ValueWrapper implements Serializable {

        private static final long serialVersionUID = 5922645096346184734L;

        private final Object value;

        public ValueWrapper(Object value) {
            this.value = value;
        }

        public Object get() {
            return this.value;
        }
    }
}
