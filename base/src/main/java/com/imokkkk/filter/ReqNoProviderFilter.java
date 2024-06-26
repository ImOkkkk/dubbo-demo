package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

import com.imokkkk.common.Constants;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * @author wyliu
 * @date 2024/6/26 13:08
 * @since 1.0
 */
@Activate(group = PROVIDER, order = -9000)
public class ReqNoProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, Object> attachments = invocation.getObjectAttachments();
        String reqTraceId =
                attachments != null ? (String) attachments.get(Constants.TRACE_ID) : null;
        reqTraceId = reqTraceId == null || reqTraceId.isEmpty() ? generateTraceId() : reqTraceId;

        // 将序列号值设置到上下文对象中
        RpcContext.getServerAttachment().setObjectAttachment(Constants.TRACE_ID, reqTraceId);
        MDC.put(Constants.TRACE_ID, reqTraceId);

        return invoker.invoke(invocation);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
