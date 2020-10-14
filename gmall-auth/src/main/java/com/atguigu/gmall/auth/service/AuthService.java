package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private GmallUmsClient umsClient;

    public void login(String loginName,
                      String password,
                      HttpServletRequest request,
                      HttpServletResponse response
    ) {

        /**
         * - 客户端携带用户名和密码请求登录 ，并携带登录前页面的路径
         * - 授权中心调用用户中心接口，根据用户名和密码查询用户信息
         * - 用户名密码不正确，不能获取用户，登录失败
         * - 如果校验成功，则生成JWT，jwt要防止别人盗取
         * - 把jwt放入cookie
         * - 为了方便页面展示登录用户昵称，向cookie中单独写入昵称（例如京东cookie中的的**unick**）
         * - 重定向 回到登录前的页面
         */
        try {
            // 1. 远程调用 获取用户信息
            ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUser(loginName, password);
            UserEntity user = userEntityResponseVo.getData();

            // 2. 用户信息判断
            if (user == null){
                throw new UserException("用户名或密码输入有误！");
            }

            // 3. 将用户信息：useId和用户名放入载荷
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", user.getId());
            map.put("username", user.getUsername());

            // 4. 防止用户token(jwt)被盗取,载荷中加入用户的ip地址
            String ipAddr = IpUtil.getIpAddressAtService(request);
            map.put("ip", ipAddr);

            // 5. 生成token(jwt)信息
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());

            // 6. 将jwt存入cookie中
            CookieUtils.setCookie(request,
                    response,
                    this.jwtProperties.getCookieName(),
                    token,
                    this.jwtProperties.getExpire() * 60);

            // 7. 将用户昵称存入cookie，用户用户登录成功后昵称回显
            CookieUtils.setCookie(request,
                    response,
                    this.jwtProperties.getUnick(),
                    user.getNickname(),
                    this.jwtProperties.getExpire() * 60);

        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("用户名或密码有误！");
        }


    }


}
