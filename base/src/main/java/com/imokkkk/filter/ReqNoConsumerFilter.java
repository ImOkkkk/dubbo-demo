package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

import com.imokkkk.common.Constants;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * @author wyliu
 * @date 2024/6/26 14:00
 * @since 1.0
 */
@Activate(group = CONSUMER, order = Integer.MIN_VALUE + 1000)
public class ReqNoConsumerFilter implements Filter, BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 从上下文对象中取出跟踪序列号值
        String existsTraceId = RpcContext.getServerAttachment().getAttachment(Constants.TRACE_ID);

        // 如果不存在 traceId，则生成一个新的 traceId
        if (existsTraceId == null || existsTraceId.isEmpty()) {
            existsTraceId = generateTraceId();
        }

        // 将序列号值设置到请求对象中
        invocation.getObjectAttachments().put(Constants.TRACE_ID, existsTraceId);
        RpcContext.getClientAttachment().setObjectAttachment(Constants.TRACE_ID, existsTraceId);

        // 将 traceId 放入 MDC 中，便于日志记录
        MDC.put(Constants.TRACE_ID, existsTraceId);

        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
