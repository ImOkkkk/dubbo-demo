package com.imokkkk.service.impl;

import com.imokkkk.facade.Provider2Facade;

import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wyliu
 * @date 2024/6/26 15:24
 * @since 1.0
 */
@DubboService
public class Provider2FacadeImpl implements Provider2Facade {

    private static final Logger logger = LoggerFactory.getLogger(Provider2FacadeImpl.class);

    @Override
    public void test1(String str) {
        logger.info(str);
    }
}
