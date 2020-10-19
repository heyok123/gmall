package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * 编写拦截器：传递登录信息-->> 将用户信息传递给后续业务
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    // 声明线程的局部变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 获取登录头信息
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());

        // 2. panduan 如果登录头中userKey为null,设置一个新的userKey存入cookie
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, jwtProperties.getUserKey(), userKey,jwtProperties.getExpireTime());
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserKey(userKey);

        // 3. 获取用户的登录信息 并解析JWT(token)
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        if (StringUtils.isNotBlank(token)) {
            try {
                // 解析jwt(token)
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                Long userId = Long.valueOf(map.get("userId").toString());
                userInfo.setUserId(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. 将信息村纳入线程的局部变量
        THREAD_LOCAL.set(userInfo);



      /*  UserInfo userInfo = new UserInfo();
        userInfo.setUserId(1L);
        userInfo.setUserKey(UUID.randomUUID().toString().substring(0,8));
        // 将信息放入多个线程的局部变量
        THREAD_LOCAL.set(userInfo);*/

        return true; // 不拦截 全部放行 只是获取用户信息
    }

    /**
     * 封装一个获取线程局部变量的方法(static)
     */
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    /**
     * 在视图渲染完成之后执行，经常在完成方法中释放资源
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 线程池-- >> 调用结束不会释放线程，复用-->>手动删除释放资源线程，否则会导致线程泄露
        THREAD_LOCAL.remove();


    }
}
