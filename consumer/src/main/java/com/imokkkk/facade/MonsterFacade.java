package com.imokkkk.facade;

import com.imokkkk.request.HereticalReq;

import org.apache.dubbo.remoting.exchange.Response;

/**
 * @author wyliu
 * @date 2024/7/24 22:42
 * @since 1.0
 */
public interface MonsterFacade {
    // 定义了一个专门处理万能修复逻辑的Dubbo接口
    Response heretical(HereticalReq req);
}
