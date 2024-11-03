package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

import cn.hutool.core.util.StrUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * @author wyliu
 * @date 2024/10/27 15:49
 * @since 1.0
 */
@Activate(group = CONSUMER)
public class ConsumerTokenFilter implements Filter {
    //方法级别层面获取配置的 auth.token 参数名
    public static final String KEY_AUTH_TOKEN = "auth.token";
    //TOKEN 字段名
    public static final String TOKEN = "TOKEN";


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String authToken = invoker.getUrl()
          .getMethodParameter(invocation.getMethodName(), KEY_AUTH_TOKEN);
        if (StrUtil.isNotBlank(authToken)){
            invocation.getObjectAttachments().put(TOKEN, authToken);
        }
        return invoker.invoke(invocation);
    }
}
