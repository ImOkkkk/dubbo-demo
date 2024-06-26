package com.imokkkk;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author wyliu
 * @date 2024/5/9 11:31
 * @since 1.0
 */
@SpringBootApplication
@EnableDubbo
public class Provider2Application {
    public static void main(String[] args){
        SpringApplication.run(Provider2Application.class, args);
    }
}
