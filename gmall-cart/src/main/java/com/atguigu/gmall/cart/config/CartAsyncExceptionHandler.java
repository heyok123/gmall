package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {

        log.error("异步调用发生异常，方法：{}，参数：{}，异常信息：{}", method, params, ex.getMessage());

        // 将异常用户信息存入redis
        String userId = params[0].toString();
        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);

        listOps.leftPush(userId);
    }
}
