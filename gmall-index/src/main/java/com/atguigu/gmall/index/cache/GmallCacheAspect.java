package com.atguigu.gmall.index.cache;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * joinPoint.getArgs(); 获取方法参数
     * joinPoint.getTarget().getClass(); 获取目标类
     * @param joinPoint
     * @return
     * @throws Throwable
     */

    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        // 获取切点方法的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取方法上的注解
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        // 获取注解的前缀
        String prefix = annotation.prefix();
        // 获取方法的参数
        Object[] args = joinPoint.getArgs();
        String param = Arrays.asList(args).toString();
        // 获取方法的返回值类型
        Class<?> returnType = method.getReturnType();

        // 1.1. 防止缓存击穿 -->> 判断缓存中是否有此数据 -->> 有此数据
        String jsonValue = this.redisTemplate.opsForValue().get(prefix + param);
        if (StringUtils.isNotBlank(jsonValue)) {
            return JSON.parseObject(jsonValue,returnType);
        }

        // 1.2. 没有此数据 加上分布式锁: 防止缓存击穿
        String lockName = annotation.lock();
        RLock lock = this.redissonClient.getFairLock(lockName + param);
        lock.lock();

        // 2.1.  判读缓存中是否有此数据 有则直接返回
        String jsonValue2 = this.redisTemplate.opsForValue().get(prefix + param);
        if (StringUtils.isNotBlank(jsonValue)) {
            lock.unlock();
            return JSON.parseObject(jsonValue,returnType);
        }

        // 2.2. 执行目标方法
        Object proceedResult = joinPoint.proceed(args);

        // 2.3. 没有数据 则查询 放入缓存
        int timeout = annotation.timeout(); // 防死锁
        int random = annotation.random(); // 防雪崩
        this.redisTemplate.opsForValue().set(prefix + param, JSON.toJSONString(proceedResult), timeout + new Random().nextInt(random), TimeUnit.MINUTES);

        // 2.4 释放分布式锁
        lock.unlock();

        // 3. 返回代理数据
        return proceedResult;


    }
}
