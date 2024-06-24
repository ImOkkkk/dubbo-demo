package com.imokkkk.service.impl;

import com.imokkkk.facade.AsyncOrderFacade;
import com.imokkkk.model.OrderInfo;

import java.util.concurrent.TimeUnit;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.AsyncContext;
import org.apache.dubbo.rpc.RpcContext;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author wyliu
 * @date 2024/6/24 14:51
 * @since 1.0
 */
@DubboService
public class AsyncOrderFacadeImpl implements AsyncOrderFacade {

    private static final Executor threadPool = Executors.newFixedThreadPool(8);

    @Override
    public OrderInfo queryOrderById(String id) {
        // 模拟执行一段耗时的业务逻辑
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        OrderInfo resultInfo =
                new OrderInfo(
                        "GeekDubbo",
                        Thread.currentThread().getName() + "#服务方异步方式之RpcContext.startAsync#" + id,
                        new BigDecimal(129));
        return resultInfo;
    }

    //使用CompletableFuture实现异步
    @Override
    public CompletableFuture<OrderInfo> queryOrderByIdFuture(String id) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Thread.sleep(5000);
                        OrderInfo resultInfo =
                                new OrderInfo(
                                        "GeekDubbo",
                                        Thread.currentThread().getName()
                                                + "#服务方异步方式之RpcContext.startAsync#"
                                                + id,
                                        new BigDecimal(129));
                        return resultInfo;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
    }

    //使用AsyncContext实现异步
    @Override
    public OrderInfo queryOrderByIdAsyncContext(String id) {
        AsyncContext asyncContext = RpcContext.startAsync();
        threadPool.execute(
                () -> {
                    asyncContext.signalContextSwitch();
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    asyncContext.write(
                            new OrderInfo(
                                    "GeekDubbo",
                                    Thread.currentThread().getName()
                                            + "#服务方异步方式之RpcContext.startAsync#"
                                            + id,
                                    new BigDecimal(129)));
                });
        return null;
    }
}
