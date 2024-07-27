package com.imokkkk.controller;

import static com.imokkkk.controller.CommonController.createReferenceConfig;

import com.alibaba.fastjson2.JSON;
import com.imokkkk.request.RepairRequest;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author wyliu
 * @date 2024/7/24 22:22
 * @since 1.0
 */
@RestController
public class MonsterController {
    // 响应码为成功时的值
    public static final String SUCC = "000000";

    @PostMapping("/gatewaygateway/repair/request")
    public Response repairRequest(@RequestBody RepairRequest repairRequest) {
        // 将入参的req转为下游方法的入参对象，并发起远程调用
        return commonInvoke(repairRequest);
    }

    private Response commonInvoke(RepairRequest repairRequest) {
        ReferenceConfig<GenericService> referenceConfig =
                createReferenceConfig(repairRequest.getClassName(), repairRequest.getUrl());
        GenericService genericService = referenceConfig.get();
        Object resp =
                genericService.$invoke(
                        repairRequest.getMtdName(),
                        new String[] {repairRequest.getParameterTypeName()},
                        new Object[] {JSON.parseObject(repairRequest.getParamsMap(), Map.class)});
        Map<String, String> respMap = JSON.to(Map.class, resp);
        Response response = new Response();
        // 判断响应对象的响应码，不是成功的话，则组装失败响应
        if (!SUCC.equals(respMap.get("respCode"))) {
            response.setStatus(Response.BAD_RESPONSE);
        } else {
            response.setStatus(Response.OK);
        }
        return response;
    }
}
