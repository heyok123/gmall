package com.atguigu.gmall.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor( @Value("${thread.pool.coreSize}")Integer coreSize,
                                                  @Value("${thread.pool.maxSize}")Integer maxPoolSize,
                                                  @Value("${thread.pool.keepalive}")Integer keepAlive,
                                                  @Value("${thread.pool.blockQueueSize}")Integer blockingQueueSize
    ){
        return new ThreadPoolExecutor(coreSize,maxPoolSize,keepAlive, TimeUnit.SECONDS,new ArrayBlockingQueue<>(blockingQueueSize));
    }

}
