package com.imokkkk.springboot.demo.consumer.task;

import com.imokkkk.springboot.demo.interfaces.DemoService;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.CommandLineRunner;

import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * @author liuwy
 * @date 2023-05-22 16:02
 * @since 1.0
 */
@Component
public class Task implements CommandLineRunner {
    @DubboReference private DemoService demoService;

    @Override
    public void run(String... args) throws Exception {
        String result = demoService.sayHello("world");
        System.out.println("Receive result ======> " + result);

        new Thread(
                        () -> {
                            while (true) {
                                try {
                                    Thread.sleep(1000);
                                    System.out.println(
                                            new Date()
                                                    + " Receive result ======> "
                                                    + demoService.sayHello("world"));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                        })
                .start();
    }
}
