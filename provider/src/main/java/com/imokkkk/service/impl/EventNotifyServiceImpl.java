package com.imokkkk.service.impl;

import com.imokkkk.service.EventNotifyService;
import org.springframework.stereotype.Component;

/**
 * @author wyliu
 * @date 2024/7/28 17:57
 * @since 1.0
 */
@Component("eventNotifyService")
public class EventNotifyServiceImpl implements EventNotifyService {

    @Override
    public void onInvoke(String str) {
        System.out.println("[事件通知][调用之前] onInvoke 执行.");
    }

    @Override
    public void onReturn(String result, String str) {
        System.out.println("[事件通知][调用之后] onReturn 执行.");

    }

    @Override
    public void onThrow(Throwable ex, String str) {
        System.out.println("[事件通知][调用异常] onThrow 执行.");
    }
}
