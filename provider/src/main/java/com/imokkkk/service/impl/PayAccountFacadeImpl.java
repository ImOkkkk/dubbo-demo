package com.imokkkk.service.impl;

import com.imokkkk.facade.PayAccountFacade;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/10/27 15:46
 * @since 1.0
 */
@DubboService(
        methods = {
            @Method(
                    name = "queryPayAccount",
                    parameters = {
                        "auth.token",
                        "123456789",
                        "auth.enable",
                        "true",
                        "auth.rsa.public.secret",
                        "queryPayAccoun_publicSecret",
                        "auth.rsa.enable",
                        "true"
                    })
        })
@Component
public class PayAccountFacadeImpl implements PayAccountFacade {

    @Override
    public String queryPayAccount(String userId) {
        String result = String.format(now() + ": Hello %s, 已查询该用户的【银行账号信息】", userId);
        System.out.println(result);
        return result;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date());
    }
}
