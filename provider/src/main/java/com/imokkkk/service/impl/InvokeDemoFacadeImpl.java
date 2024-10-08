package com.imokkkk.service.impl;

import com.imokkkk.DemoService;
import com.imokkkk.facade.InvokeDemoFacade;
import com.imokkkk.facade.ValidationFacade;
import com.imokkkk.model.ValidateUserInfo;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/7/28 17:56
 * @since 1.0
 */
@Component
@DubboService
public class InvokeDemoFacadeImpl implements InvokeDemoFacade {

    @DubboReference(validation = "jvalidation")
    private ValidationFacade validationFacade;

    @Autowired
    @DubboReference(
            /** 接口调研超时时间，1毫秒 * */
            timeout = 10000,
            /** 启动时不检查 DemoFacade 是否能正常提供服务 * */
            check = false,

            /** 为 DemoFacade 的 print 方法设置事件通知机制 * */
            methods = {
                @Method(
                        name = "print",
                        oninvoke = "eventNotifyService.onInvoke",
                        onreturn = "eventNotifyService.onReturn",
                        onthrow = "eventNotifyService.onThrow")
            })
    private DemoService demoService;

    // 简单的一个 hello 方法，然后内部调用下游Dubbo接口 DemoFacade 的 print 方法
    public String hello(String str) {
        return demoService.print(str);
    }

    // 一个简单的触发调用下游 ValidationFacade.validateUser 的方法
    public String invokeValidate(String id, String name, String sex) {
        return validationFacade.validateUser(new ValidateUserInfo(id, name, sex));
    }
}
