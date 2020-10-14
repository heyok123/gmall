package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            @Value("${threadPool.corePoolSize}") Integer corePoolSize,
            @Value("${threadPool.maximumPoolSize}") Integer maximumPoolSize,
            @Value("${threadPool.keepAliveTime}") Integer keepAliveTime,
            @Value("${threadPool.blockingSize}") Integer blockingSize
    ){

        return  new ThreadPoolExecutor(corePoolSize, maximumPoolSize,keepAliveTime,TimeUnit.SECONDS,new ArrayBlockingQueue<>(blockingSize) , Executors.defaultThreadFactory(),(Runnable r, ThreadPoolExecutor executor) -> {
            System.out.println("您的请求被拒绝了");
        });
    }

}
