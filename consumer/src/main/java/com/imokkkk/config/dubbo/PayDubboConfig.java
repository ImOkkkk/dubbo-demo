package com.imokkkk.config.dubbo;

import com.imokkkk.facade.PayAccountFacade;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wyliu
 * @date 2025/7/12 21:25
 * @since 1.0
 */
@Configuration
public class PayDubboConfig {

    @DubboReference(
            timeout = 10000,
            methods = {
                @Method(
                        name = "queryPayAccount",
                        parameters = {"auth.token", "123456789"})
            })
    private PayAccountFacade payAccountFacade;

    @Bean
    @ConditionalOnMissingBean(name = "payAccountFacade")
    public PayAccountFacade payAccountFacade() {
        return payAccountFacade;
    }
}
