package com.imokkkk.service.impl;

import com.imokkkk.facade.InvokeCacheFacade;
import com.imokkkk.facade.MenuQueryFacade;
import com.imokkkk.facade.RoleQueryFacade;
import com.imokkkk.facade.UserQueryFacade;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author wyliu
 * @date 2024/8/11 11:26
 * @since 1.0
 */
@DubboService
@Component
public class InvokeCacheFacadeImpl implements InvokeCacheFacade {

    // 引用下游查询菜单信息列表的接口，没有添加缓存属性
    @DubboReference private UserQueryFacade userQueryFacade;

    // 引用下游查询菜单信息列表的接口，添加 cache = lru 属性
    @DubboReference(cache = "lru")
    private MenuQueryFacade menuQueryFacade;

    @DubboReference(cache = "jcache")
    private RoleQueryFacade roleQueryFacade;

    // 定义的一个线程池，来模拟网关接收了很多请求
    ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void invokeCache() {
        // 循环 3 次，模拟网关被 App 请求调用了 3 次
        for (int i = 0; i < 5; i++) {
            int index = i;
            executorService.execute(() -> invokeCacheInner(index));
        }
    }

    private void invokeCacheInner(int i) {
        String authMenuList = menuQueryFacade.queryAuthorizedMenuList("1");
        String roleList = roleQueryFacade.queryRoleList("1");
        String user = userQueryFacade.queryUser("1");

        // 打印远程调用的结果，看看是走缓存还是走远程
        String idx = new DecimalFormat("00").format(i);
        System.out.println("第 " + idx + " 次调用【角色信息列表】结果为: " + roleList);
        System.out.println("第 " + idx + " 次调用【菜单信息列表】结果为: " + authMenuList);
        System.out.println("第 " + idx + " 次调用【登录用户简情】结果为: " + user);
        System.out.println();
    }
}
