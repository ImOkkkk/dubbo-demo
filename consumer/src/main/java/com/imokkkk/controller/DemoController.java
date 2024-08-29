package com.imokkkk.controller;

import com.alibaba.fastjson2.JSON;
import com.imokkkk.DemoService;
import com.imokkkk.facade.AsyncOrderFacade;
import com.imokkkk.facade.InvokeCacheFacade;
import com.imokkkk.facade.InvokeDemoFacade;
import com.imokkkk.facade.ValidationFacade;
import com.imokkkk.model.OrderInfo;

import com.imokkkk.model.ValidateUserInfo;
import org.apache.dubbo.common.constants.ClusterRules;
import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author wyliu
 * @date 2024/6/24 10:39
 * @since 1.0
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @DubboReference(
            timeout = 3000,
            retries = 3,
            cluster = ClusterRules.FAIL_BACK,
            loadbalance = LoadbalanceRules.ROUND_ROBIN)
    private DemoService demoService;

    @DubboReference
    private InvokeDemoFacade invokeDemoFacade;

    @DubboReference
    private ValidationFacade validationFacade;

    @DubboReference
    private InvokeCacheFacade invokeCacheFacade;


    @DubboReference(timeout = 100000)
    private AsyncOrderFacade asyncOrderFacade;

    // http://127.0.0.1:6325/demo/print?str=hello
    @GetMapping("/print")
    public String print(@RequestParam("str") String str) {
        return demoService.print(str);
    }

    @GetMapping("/print1")
    public String print1(@RequestParam("str") String str) {
        return invokeDemoFacade.hello(str);
    }

    @GetMapping("/print2")
    public String print2() {
        ValidateUserInfo validateUserInfo = new ValidateUserInfo();
        validateUserInfo.setName("AA");
        return validationFacade.validateUser(validateUserInfo);
    }

    @GetMapping("/print3")
    public String print3() {
        invokeCacheFacade.invokeCache();
        return "SUCCESS";
    }

    // http://127.0.0.1:6325/demo/queryOrderById?id=1&async=true
    @GetMapping("/queryOrderById")
    public String queryOrderById(
            @RequestParam("id") String id, @RequestParam("async") Boolean async)
            throws ExecutionException, InterruptedException {
//        OrderInfo orderInfo =
//                async
//                        // ? asyncOrderFacade.queryOrderByIdFuture(id).get()
//                        ? asyncOrderFacade.queryOrderByIdAsyncContext(id)
//                        : asyncOrderFacade.queryOrderById(id);
//        return JSON.toJSONString(orderInfo);

        // Consumer异步调用
        CompletableFuture<OrderInfo> future =
                CompletableFuture.supplyAsync(
                        () -> {
                            return asyncOrderFacade.queryOrderById(id);
                        });
        future.whenComplete(
                (v, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                    } else {
                        logger.info("Response: " + JSON.toJSONString(v));
                    }
                });
        return JSON.toJSONString(future.get());
    }
}
