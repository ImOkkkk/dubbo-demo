package com.imokkkk.facade.impl;

import cn.hutool.extra.spring.SpringUtil;

import com.imokkkk.facade.MonsterFacade;
import com.imokkkk.request.HereticalReq;

import groovy.lang.GroovyClassLoader;

import org.apache.dubbo.remoting.exchange.Response;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author wyliu
 * @date 2024/7/24 22:46
 * @since 1.0
 */
public class MonsterFacadeImpl implements MonsterFacade {

    private GroovyClassLoader groovyClassLoader;

    @Override
    public Response heretical(HereticalReq req) {
        // 编译 Java 代码，然后变成 JVM 可识别的 Class 对象信息
        Class<?> javaClass = compile(req.getJavaCode());

        // 为 Class 对象信息，自定义一个名称，将来创建 Spring 单例对象要用到
        String beanName = "Custom" + javaClass.getSimpleName();

        // 通过 Spring 来创建单例对象
        generateSpringBean(beanName, javaClass);

        // 获取 beanName 对应的单例对象
        MonsterInvokeRunnable runnable =
                (MonsterInvokeRunnable) SpringUtil.getBean(beanName, javaClass);

        // 执行单例对象的方法即可
        Object resp = runnable.run(req.getReqParamsMap());

        // 返回结果
        Response response = new Response();
        response.setResult(resp);
        return response;
    }

    // 利用 groovy-all.jar 中的 groovyClassLoader 来编译 Java 代码
    private Class<?> compile(String javaCode) {
        return groovyClassLoader.parseClass(javaCode);
    }

    // 生成Spring容器Bean对象
    private void generateSpringBean(String beanName, Class<?> javaClass) {
        // 构建 Bean 定义对象
        BeanDefinitionBuilder beanDefinitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(javaClass);
        AbstractBeanDefinition rawBeanDefinition = beanDefinitionBuilder.getRawBeanDefinition();

        // 将 bean 移交给 Spring 去管理
        ConfigurableApplicationContext appCtx =
                (ConfigurableApplicationContext) SpringUtil.getApplicationContext();
        appCtx.getAutowireCapableBeanFactory()
                .applyBeanPostProcessorsAfterInitialization(rawBeanDefinition, beanName);
        ((BeanDefinitionRegistry) appCtx.getBeanFactory())
                .registerBeanDefinition(beanName, rawBeanDefinition);
    }

    // 下面的空实现仅示意代码思路而已
    private class MonsterAction {}

    private class MonsterInvokeRunnable {
        public Object run(Object reqParamsMap) {
            return null;
        }
    }
}
