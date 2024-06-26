package com.imokkkk.service.impl;

import com.imokkkk.facade.AsyncOrderFacade;
import com.imokkkk.facade.Provider2Facade;
import com.imokkkk.model.OrderInfo;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.AsyncContext;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author wyliu
 * @date 2024/6/24 14:51
 * @since 1.0
 */
@DubboService
public class AsyncOrderFacadeImpl implements AsyncOrderFacade {
    private static final Logger logger = LoggerFactory.getLogger(AsyncOrderFacadeImpl.class);

    private static final Executor threadPool = Executors.newFixedThreadPool(8);

    @DubboReference(timeout = 100000) private Provider2Facade provider2Facade;

    @Override
    public OrderInfo queryOrderById(String id) {
        // 模拟执行一段耗时的业务逻辑
        try {
            provider2Facade.test1("do something...");
            logger.info("queryOrderById...");
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

    // 使用CompletableFuture实现异步
    @Override
    public CompletableFuture<OrderInfo> queryOrderByIdFuture(String id) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        logger.info("queryOrderById future...");
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

    // 使用AsyncContext实现异步
    @Override
    public OrderInfo queryOrderByIdAsyncContext(String id) {
        AsyncContext asyncContext = RpcContext.startAsync();
        threadPool.execute(
                () -> {
                    asyncContext.signalContextSwitch();
                    try {
                        logger.info("queryOrderById asyncContext...");
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
