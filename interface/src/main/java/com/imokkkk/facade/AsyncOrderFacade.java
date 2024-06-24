package com.imokkkk.facade;

import com.imokkkk.model.OrderInfo;
import java.util.concurrent.CompletableFuture;

/**
 * @author wyliu
 * @date 2024/6/24 14:47
 * @since 1.0
 */
public interface AsyncOrderFacade {
    OrderInfo queryOrderById(String id);

    CompletableFuture<OrderInfo> queryOrderByIdFuture(String id);

    OrderInfo queryOrderByIdAsyncContext(String id);
}
