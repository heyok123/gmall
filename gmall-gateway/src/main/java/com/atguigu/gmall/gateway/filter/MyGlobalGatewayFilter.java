package com.atguigu.gmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局过滤器
 */

@Component
public class MyGlobalGatewayFilter implements GatewayFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("全局过滤器 无需配置 -->> 拦截经过网关的所有请求");
        return chain.filter(exchange); // 放行
    }

//    通过实现Orderer接口的getOrder方法控制全局过滤器的执行顺序
    @Override
    public int getOrder() {
        return 0;
    }
}
