package com.imokkkk.service.impl;

import com.imokkkk.DemoService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @author wyliu
 * @date 2024/6/24 10:33
 * @since 1.0
 */
@DubboService(timeout = 3000)
public class DemoServiceImpl implements DemoService {

    @Override
    public String print(String str) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return str;
    }
}
