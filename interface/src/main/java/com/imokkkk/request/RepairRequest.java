package com.imokkkk.request;

import lombok.Data;

/**
 * @author wyliu
 * @date 2024/7/24 22:30
 * @since 1.0
 */
@Data
public class RepairRequest {
    /**
     *
     *
     * <h2>接口类名，例:com.xyz.MonsterFacade</h2>
     *
     * *
     */
    private String className;

    /**
     *
     *
     * <h2>接口方法名，例:heretical</h2>
     *
     * *
     */
    private String mtdName;

    /**
     *
     *
     * <h2>接口方法参数类名，例:com.xyz.bean.HereticalReq</h2>
     *
     * *
     */
    private String parameterTypeName;

    /**
     *
     *
     * <h2>指定的URL节点，例:dubbo://ip:port</h2>
     *
     * *
     */
    private String url;

    /**
     *
     *
     * <h2>可以是调用具体接口的请求参数，也可以是修复问题的Java代码</h2>
     *
     * *
     */
    private String paramsMap;
}
