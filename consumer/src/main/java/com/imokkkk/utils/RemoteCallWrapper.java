package com.imokkkk.utils;

import com.alibaba.fastjson2.JSON;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author wyliu
 * @date 2025/7/12 21:40
 * @since 1.0
 */
public class RemoteCallWrapper {
    private static Logger logger = LoggerFactory.getLogger(RemoteCallWrapper.class);

    private static ImmutableSet<String> SUCCESS_CHECK_METHOD =
            ImmutableSet.of("isSuccess", "getSuccess");

    private static ImmutableSet<String> SUCCESS_CODE_METHOD =
            ImmutableSet.of("getCode", "getResponseCode");

    private static ImmutableSet<String> SUCCESS_CODE = ImmutableSet.of("0", "200");

    public static <T, R> R call(Function<T, R> function, T request, boolean checkResponse) {
        return call(function, request, request.getClass().getSimpleName(), checkResponse, false);
    }

    public static <T, R> R call(Function<T, R> function, T request) {
        return call(function, request, request.getClass().getSimpleName(), true, false);
    }

    public static <T, R> R call(Function<T, R> function, T request, String requestName) {
        return call(function, request, requestName, true, false);
    }

    public static <T, R> R call(
            Function<T, R> function, T request, boolean checkResponse, boolean checkResponseCode) {
        return call(
                function,
                request,
                request.getClass().getSimpleName(),
                checkResponse,
                checkResponseCode);
    }

    public static <T, R> R call(
            Function<T, R> function,
            T request,
            String requestName,
            boolean checkResponse,
            boolean checkResponseCode) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        R response = null;
        try {
            response = function.apply(request);
            stopwatch.stop();
            if (checkResponse) {
                Assert.notNull(response, "response is null");
                if (!isResponseSuccess(response)) {
                    logger.error(
                            "response is not success, request: {}, response: {}",
                            com.alibaba.fastjson2.JSON.toJSONString(request),
                            JSON.toJSONString(response));
                    throw new RuntimeException(
                            JSON.toJSONString(response) + "response is not success");
                }
            }
            if (checkResponseCode) {
                Assert.notNull(response, "response is null");
                if (!isResponseSuccessCode(response)) {
                    logger.error(
                            "response code is not success code, request: {}, response: {}",
                            JSON.toJSONString(request),
                            JSON.toJSONString(response));
                    throw new RuntimeException(
                            JSON.toJSONString(response) + "response code is not success code");
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("call function error", e);
            throw new RuntimeException(e);
        } catch (Throwable e) {
            logger.error("call function error", e);
            throw new RuntimeException(e);
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info(
                        "## Method={} ,## 耗时={}ms ,## [请求报文]:{},## [响应报文]:{}",
                        requestName,
                        stopwatch.elapsed(TimeUnit.MILLISECONDS),
                        JSON.toJSONString(request),
                        JSON.toJSONString(response));
            }
        }
        return response;
    }

    private static <R> boolean isResponseSuccess(R response)
            throws InvocationTargetException, IllegalAccessException {
        Method successMethod = null;
        Method[] methods = response.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (SUCCESS_CHECK_METHOD.contains(methodName)) {
                successMethod = method;
                break;
            }
        }
        if (successMethod == null) {
            return true;
        }
        return (Boolean) successMethod.invoke(response);
    }

    private static <R> boolean isResponseSuccessCode(R response)
            throws InvocationTargetException, IllegalAccessException {
        Method successMethod = null;
        Method[] methods = response.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (SUCCESS_CODE_METHOD.contains(methodName)) {
                successMethod = method;
                break;
            }
        }
        if (successMethod == null) {
            return true;
        }
        return SUCCESS_CODE.contains(successMethod.invoke(response));
    }
}
