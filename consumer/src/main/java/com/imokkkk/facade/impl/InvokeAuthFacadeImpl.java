package com.imokkkk.facade.impl;

import com.imokkkk.facade.InvokeAuthFacade;
import com.imokkkk.facade.PayAccountFacade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wyliu
 * @date 2024/10/27 16:01
 * @since 1.0
 */
@Service
public class InvokeAuthFacadeImpl implements InvokeAuthFacade {
    @Autowired
    private PayAccountFacade payAccountFacade;

    @Override
    public void invokeAuth() {
        String respMsg = payAccountFacade.queryPayAccount("Geek");
        System.out.println(respMsg);
    }
}
