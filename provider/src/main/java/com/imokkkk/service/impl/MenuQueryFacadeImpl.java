package com.imokkkk.service.impl;

import com.imokkkk.facade.MenuQueryFacade;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/8/11 11:40
 * @since 1.0
 */
@DubboService
@Component
public class MenuQueryFacadeImpl implements MenuQueryFacade {

    @Override
    public String queryAuthorizedMenuList(String userId) {
        String result =
                String.format(
                        System.currentTimeMillis() + ": Hello %s, 已查询该用户已授权的【菜单列表信息】", userId);
        System.out.println(result);
        return result;
    }
}
