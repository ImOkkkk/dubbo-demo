package com.imokkkk.service.impl;

import com.imokkkk.facade.UserQueryFacade;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/8/11 11:42
 * @since 1.0
 */
@DubboService
@Component
public class UserQueryFacadeImpl implements UserQueryFacade {

    @Override
    public String queryUser(String name) {
        String result = String.format(System.currentTimeMillis() + ": Hello %s, 已查询【用户简单信息】", name);
        System.out.println(result);
        return result;
    }
}
