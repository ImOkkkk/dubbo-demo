package com.imokkkk.springboot.demo.provider.service.impl;

import com.imokkkk.springboot.demo.interfaces.DemoService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @author liuwy
 * @date 2023-05-22 13:24
 * @since 1.0
 */
@DubboService
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello" + name;
    }
}
