package com.imokkkk.service.impl;

import com.imokkkk.facade.ValidationFacade;
import com.imokkkk.model.ValidateUserInfo;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/7/29 21:33
 * @since 1.0
 */
@DubboService(validation = "jvalidation")
@Component
public class ValidationFacadeImpl implements ValidationFacade {

    @Override
    public String validateUser(ValidateUserInfo userInfo) {
        // 这里就象征性的模拟下业务逻辑
        String retMsg =
                "Ret: " + userInfo.getId() + "," + userInfo.getName() + "," + userInfo.getSex();
        System.out.println(retMsg);
        return retMsg;
    }
}
