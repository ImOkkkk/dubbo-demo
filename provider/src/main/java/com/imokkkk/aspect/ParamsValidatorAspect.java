package com.imokkkk.aspect;

import org.apache.dubbo.common.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * @author wyliu
 * @date 2024/8/11 12:59
 * @since 1.0
 */
@Order(Integer.MIN_VALUE)
@Aspect
@Component
public class ParamsValidatorAspect {
    // 定义参数校验器接口
    private static Validator validator;

    static {
        // 通过工厂创建参数校验器
        ValidatorFactory factory =
                Validation.byProvider(HibernateValidator.class)
                        .configure()
                        .failFast(true)
                        .messageInterpolator(
                                new ResourceBundleMessageInterpolator(
                                        new AggregateResourceBundleLocator(
                                                Arrays.asList("validationMessage"))))
                        .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @Pointcut("execution(* com.imokkkk.service.impl..*FacadeImpl.* (..))")
    protected void executeService() {}

    @Around("executeService()")
    public Object doAround(ProceedingJoinPoint pjp) {

        try {
            // 参数校验，如果校验不通过的话，会直接抛异常
            validateParams(pjp.getArgs());
            // pjp.proceed() 这行代码才是真正核心业务逻辑
            // 比如：ValidationFacadeImpl.validateUser 就在这里被执行的
            Object result = pjp.proceed();
            // 正常响应
            return buildNormalResp(result);
        } catch (Throwable e) {
            // 异常响应，可以考虑怎么统一包装异常响应的统一数据格式
            return buildExceptionResp(e);
        }
    }

    private Object buildExceptionResp(Throwable e) {
        return null;
    }

    private Object buildNormalResp(Object result) {
        return null;
    }

    // 参数校验
    private static void validateParams(Object[] args) {
        for (Object obj : args) {
            String errorMsg = validate(obj);
            if (StringUtils.isNotBlank(errorMsg)) {
                throw new RuntimeException("参数校验不通过: " + errorMsg);
            }
        }
    }

    // 校验入参对象，核心逻辑都是调用 hibernate-validator 插件里面的方法
    private static String validate(Object obj) {
        if (null == obj) {
            return "校验对象不能为空";
        }
        StringBuilder message = new StringBuilder();
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(obj);
        int size = constraintViolations.size();
        if (size == 0) {
            return "";
        }
        int index = 0;
        for (ConstraintViolation<Object> violation : constraintViolations) {
            message.append(violation.getPropertyPath()).append(" ").append(violation.getMessage());
            index++;
            if (index < size) {
                message.append(";");
            }
        }
        return message.toString();
    }
}
