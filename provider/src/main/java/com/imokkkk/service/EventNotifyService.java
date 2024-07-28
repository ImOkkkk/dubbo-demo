package com.imokkkk.service;

/**
 * @author wyliu
 * @date 2024/7/28 17:57
 * @since 1.0
 */
public interface EventNotifyService {

    /**
     * 调用之前
     * @param str
     */
    void onInvoke(String str);

    /**
     * 有参数：调用之后
     * @param result 第一个参数 接收 [事件通知]服务接口的方法返回值类型保持一致
     * @param str 第二个或者之后，与[事件通知]服务接口的方法入参保持一致
     */
    void onReturn(String result, String str);

    /**
     * 抛异常
     * @param ex
     * @param name
     */
    void onThrow(Throwable ex, String str);
}
