package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

import com.imokkkk.util.OpsUtils;
import com.imokkkk.util.SignUtils;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author wyliu
 * @date 2024/10/27 21:06
 * @since 1.0
 */
@Activate(group = CONSUMER)
public class ConsumerAddSignFilter implements Filter {
    // SING 字段名
    public static final String SING = "SING";

    // 方法级别层面获取配置的 auth.rsa.private.secret 参数名
    public static final String KEY_AUTH_RSA_PRIVATE_SECRET = "auth.rsa.private.secret";

    @Value(value = "${privateKey}")
    private String privateKey;

    @Value(value = "${publicKey}")
    private String publicKey;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 从方法层面获取 auth.token 参数值
        String aesSecretOpsKey =
                invoker.getUrl()
                        .getMethodParameter(
                                invocation.getMethodName(), KEY_AUTH_RSA_PRIVATE_SECRET);
        // 从 OPS 配置中心里面获取到 aesSecretOpsKey 对应的密钥值
        String privateKey = OpsUtils.get(aesSecretOpsKey);
        // 加签处理
        String sign = SignUtils.addSign(invocation.getArguments(), privateKey);
        // sign 不为空的话则设置到请求对象中
        if (StringUtils.isNotBlank(sign)) {
            invocation.getObjectAttachments().put(SING, sign);
        }
        // 继续后面过滤器的调用
        return invoker.invoke(invocation);
    }
}
