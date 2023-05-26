package com.imokkkk.springboot.demo.consumer.task;

import com.imokkkk.springboot.demo.interfaces.DemoService;
import com.imokkkk.springboot.demo.interfaces.WrapperGreeter;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author liuwy
 * @date 2023-05-22 16:02
 * @since 1.0
 */
@Component
public class Task implements CommandLineRunner {
    @DubboReference private DemoService demoService;

    @DubboReference private WrapperGreeter wrapperGreeter;

    @Override
    public void run(String... args) throws Exception {
        //        String result = demoService.sayHello("world");
        //        System.out.println("Receive result ======> " + result);

        // Triple
        wrapperGreeter.sayHelloServerStream(
                "server stream",
                new StreamObserver<String>() {
                    @Override
                    public void onNext(String data) {
                        System.out.println(data);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("onCompleted");
                    }
                });

        StreamObserver<String> request =
                wrapperGreeter.sayHelloStream(
                        new StreamObserver<String>() {
                            @Override
                            public void onNext(String data) {
                                System.out.println(data);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                throwable.printStackTrace();
                            }

                            @Override
                            public void onCompleted() {
                                System.out.println("onCompleted");
                            }
                        });
        for (int i = 0; i < 10; i++) {
            request.onNext("stream request" + i);
        }
        request.onCompleted();
    }
}
