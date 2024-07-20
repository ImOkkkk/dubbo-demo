package com.imokkkk.controller;

import com.alibaba.fastjson2.JSON;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author wyliu
 * @date 2024/7/16 21:53
 * @since 1.0
 */
@RestController
public class CommonController {
    public static final String SUCC = "000000";

    // 定义URL地址
    @PostMapping("/gateway/{className}/{mtdName}/{parameterTypeName}/request")
    public Response commonRequest(
            @PathVariable String className,
            @PathVariable String mtdName,
            @PathVariable String parameterTypeName,
            @RequestBody String requestBody) {
        // 将入参的req转为下游方法的入参对象，并发起远程调用
        return commonInvoke(className, mtdName, parameterTypeName, requestBody);
    }

    /**
     *
     *
     * <h2>模拟公共的远程调用方法.</h2>
     *
     * @param className:下游的接口归属方法的全类名。
     * @param mtdName:下游接口的方法名。
     * @param parameterTypeName:下游接口的方法入参的全类名。
     * @param requestBody:需要请求到下游的数据。
     * @return 直接返回下游的整个对象。
     */
    private Response commonInvoke(
            String className, String mtdName, String parameterTypeName, String requestBody) {
        ReferenceConfig<GenericService> referenceConfig = createReferenceConfig(className);
        GenericService genericService = referenceConfig.get();
        Object resp =
                genericService.$invoke(
                        mtdName,
                        new String[] {parameterTypeName},
                        new Object[] {JSON.parseObject(requestBody, Map.class)});
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

    private static ReferenceConfig<GenericService> createReferenceConfig(String className) {
        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance();
        // 设置应用服务名称
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName(dubboBootstrap.getApplicationModel().getApplicationName());
        // 设置注册中心地址
        String address =
                dubboBootstrap.getConfigManager().getRegistries().iterator().next().getAddress();
        RegistryConfig registryConfig = new RegistryConfig(address);
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setRegistry(registryConfig);
        referenceConfig.setInterface(className);
        // 设置泛化x调用形式
        referenceConfig.setGeneric(true);
        // 设置默认超时时间
        referenceConfig.setTimeout(5 * 1000);
        return referenceConfig;
    }
}
