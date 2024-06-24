package com.imokkkk.controller;

import com.imokkkk.DemoService;

import org.apache.dubbo.common.constants.ClusterRules;
import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wyliu
 * @date 2024/6/24 10:39
 * @since 1.0
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    @DubboReference(timeout = 3000, retries = 3, cluster = ClusterRules.FAIL_BACK, loadbalance = LoadbalanceRules.ROUND_ROBIN)
    private DemoService demoService;

    @GetMapping("/print")
    public String print(@RequestParam("str") String str) {
        return demoService.print(str);
    }
}
