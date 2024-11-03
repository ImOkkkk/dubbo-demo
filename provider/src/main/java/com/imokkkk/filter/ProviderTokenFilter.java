package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Map;

/**
 * @author wyliu
 * @date 2024/10/27 15:38
 * @since 1.0
 */
@Activate(group = PROVIDER)
public class ProviderTokenFilter implements Filter {
    // TOKEN 字段名
    public static final String TOKEN = "TOKEN";

    // 方法级别层面获取配置的 auth.enable 参数名
    public static final String KEY_AUTH_ENABLE = "auth.enable";

    // 方法级别层面获取配置的 auth.token 参数名
    public static final String KEY_AUTH_TOKEN = "auth.token";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String authEnable =
                invoker.getUrl().getMethodParameter(invocation.getMethodName(), KEY_AUTH_ENABLE);
        if (!Boolean.TRUE.toString().equals(authEnable)) {
            return invoker.invoke(invocation);
        }
        Map<String, Object> attachments = invocation.getObjectAttachments();
        String recvToken = attachments != null ? (String) attachments.get(TOKEN) : null;
        // 既然需要认证，如果收到的 TOKEN 为空，则直接抛异常
        if (StringUtils.isBlank(recvToken)) {
            throw new RuntimeException(
                    "Recv token is null or empty, path: "
                            + String.join(
                                    ".",
                                    invoker.getInterface().getName(),
                                    invocation.getMethodName()));
        }
        String authToken =
                invoker.getUrl().getMethodParameter(invocation.getMethodName(), KEY_AUTH_TOKEN);
        if (!recvToken.equals(authToken)) {
            throw new RuntimeException(
                    "Recv token is invalid, path: "
                            + String.join(
                                    ".",
                                    invoker.getInterface().getName(),
                                    invocation.getMethodName()));
        }
        return invoker.invoke(invocation);
    }
}
