package com.imokkkk.springboot.demo.interfaces;

import org.apache.dubbo.common.stream.StreamObserver;

/**
 * @author liuwy
 * @date 2023-05-26 13:44
 * @since 1.0
 */
public interface WrapperGreeter {
    StreamObserver<String> sayHelloStream(StreamObserver<String> response);

    void sayHelloServerStream(String request, StreamObserver<String> response);
}
