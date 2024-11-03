package com.imokkkk.facade.impl;

import com.imokkkk.facade.InvokeAuthFacade;
import com.imokkkk.facade.PayAccountFacade;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.stereotype.Service;

/**
 * @author wyliu
 * @date 2024/10/27 16:01
 * @since 1.0
 */
@Service
public class InvokeAuthFacadeImpl implements InvokeAuthFacade {
    @DubboReference(
            timeout = 10000,
            methods = {
                @Method(
                        name = "queryPayAccount",
                        parameters = {"auth.token", "123456789"})
            })
    private PayAccountFacade payAccountFacade;

    @Override
    public void invokeAuth() {
        String respMsg = payAccountFacade.queryPayAccount("Geek");
        System.out.println(respMsg);
    }
}
