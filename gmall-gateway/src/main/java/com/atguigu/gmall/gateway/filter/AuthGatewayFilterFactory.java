package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 自定义局部过滤器
 */

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 接收局部过滤器的参数：
     * 			1.定义一个config静态内部类
     * 			2.继承过滤器的抽象工厂类时，指定自定义config类的泛型
     * 			3.重写过滤器的无参构造方法，调用了父类的构造方法传入自定义config类的class
     * 			4.重写shortcutFiledOrder方法指定了config类中的字段顺序
     * 			5.如果使用集合接收过滤器的参数，还需要重写shortcutType方法
     */

    //    重写过滤器的无参构造方法
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                System.out.println("自定义局部过滤器");

                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1. 请求判断：路径是否在白名单中，不在直接放行
                String path = request.getURI().getPath();
                if (config.authPathes.stream().allMatch(authPath -> path.indexOf(authPath) == -1)){
                    return chain.filter(exchange);
                }

                // 2. 请求路径在白名单 -->> 获取token信息：同步请求cookie中获取/异步请求header中获取
                String token = request.getHeaders().getFirst("token");
                if (StringUtils.isEmpty(token)) {
                    // 头信息没有 --> cookie中获取
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                    }
                    // 3. token信息判断 -->> 为空直接拦截 -->> 重定向 登录
                    if (StringUtils.isEmpty(token)) {
//                        // 303状态码表示由于请求对应的资源存在着另一个URI，应使用重定向获取请求的资源
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION,
                                "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();
                    }

                    try {
                        // 4. 解析token -->> 有异常直接拦截
                        Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                        // 5. ip判断 -->> 防止盗用
                        String ip = (String) map.get("ip");
                        String ipAddr = IpUtil.getIpAddressAtGateway(request);
                        if (!StringUtils.equals(ip, ipAddr)){
                            // 重定向 登录
                            response.setStatusCode(HttpStatus.SEE_OTHER);
                            response.getHeaders().set(HttpHeaders.LOCATION,
                                    "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                            return response.setComplete();
                        }
                        // 6. 传递登录信息到后续服务器 无需再解析token(jwt)
                        // 将userId转变成request对象。mutate：转变的意思
                        request.mutate().header("userId", map.get("userId").toString()).build();
                        // 将新的request对象转变成exchange对象
                        exchange.mutate().request(request).build();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 重定向 登录
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION,
                                "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();
                    }
                }

                // 7. 放行
                return chain.filter(exchange);
            }
        };
    }

    /**
     * config的静态内部类：读取配置内容
     */
    public static class PathConfig{
        private List<String> authPathes;
    }

    /**
     * 重写shortcutFiledOrder方法: 指定了config类中的字段顺序
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("authPathes");
    }

    /**
     * 重写shortcutType方法
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }
}
