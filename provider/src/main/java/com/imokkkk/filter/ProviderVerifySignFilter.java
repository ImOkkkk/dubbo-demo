package com.imokkkk.filter;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

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

import java.util.Map;

/**
 * @author wyliu
 * @date 2024/10/27 20:17
 * @since 1.0
 */
@Activate(PROVIDER)
public class ProviderVerifySignFilter implements Filter {
    // SING 字段名
    public static final String SING = "SING";
    // 方法级别层面获取配置的 auth.ras.enable 参数名
    public static final String KEY_AUTH_RSA_ENABLE = "auth.rsa.enable";
    // 方法级别层面获取配置的 auth.rsa.public.secret 参数名
    public static final String KEY_AUTH_RSA_PUBLIC_SECRET = "auth.rsa.public.secret";

    @Value(value = "${privateKey}")
    private String privateKey;

    @Value(value = "${publicKey}")
    private String publicKey;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String singEnable =
                invoker.getUrl()
                        .getMethodParameter(invocation.getMethodName(), KEY_AUTH_RSA_ENABLE);
        if (!Boolean.TRUE.toString().equals(singEnable)) {
            return invoker.invoke(invocation);
        }
        Map<String, Object> attachments = invocation.getObjectAttachments();
        String recvSign = attachments != null ? (String) attachments.get(SING) : null;
        // 既然需要认证，如果收到的加签值为空的话，则直接抛异常
        if (StringUtils.isBlank(recvSign)) {
            throw new RuntimeException(
                    "Recv sign is null or empty, path: "
                            + String.join(
                                    ".",
                                    invoker.getInterface().getName(),
                                    invocation.getMethodName()));
        }
        // 从方法层面获取 auth.rsa.public.secret 参数值
        String rsaPublicSecretOpsKey = invoker.getUrl().getMethodParameter
          (invocation.getMethodName(), KEY_AUTH_RSA_PUBLIC_SECRET);
        // 从 OPS 配置中心里面获取到 rsaPublicSecretOpsKey 对应的密钥值
        String publicKey = OpsUtils.get(rsaPublicSecretOpsKey);
        // 加签处理
        boolean passed = SignUtils.verifySign(invocation.getArguments(), publicKey, recvSign);
        if (!passed) {
            throw new RuntimeException(
                    "Recv sign is invalid, path: "
                            + String.join(
                                    ".",
                                    invoker.getInterface().getName(),
                                    invocation.getMethodName()));
        }

        return invoker.invoke(invocation);
    }
}
