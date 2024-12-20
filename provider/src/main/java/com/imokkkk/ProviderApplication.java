package com.imokkkk;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author wyliu
 * @date 2024/5/9 11:31
 * @since 1.0
 */
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class ProviderApplication {
    public static void main(String[] args){
        SpringApplication.run(ProviderApplication.class, args);
    }
}
