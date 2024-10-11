package com.imokkkk.service.impl;

import com.imokkkk.facade.RoleQueryFacade;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/8/11 11:30
 * @since 1.0
 */

// 配合限流过滤器而添加的限流参数 qps.enable、qps.value
@DubboService(
        methods = {
            @Method(
                    name = "queryRoleList",
                    parameters = {"qps.enable", "true", "qps.value", "3", "qps.type", "rlimit"})
        })
@Component
public class RoleQueryFacadeImpl implements RoleQueryFacade {

    @Override
    public String queryRoleList(String userId) {
        String result =
                String.format(System.currentTimeMillis() + ": Hello %s, 已查询该用户【角色列表信息】", userId);
        System.out.println(result);
        return result;
    }
}
