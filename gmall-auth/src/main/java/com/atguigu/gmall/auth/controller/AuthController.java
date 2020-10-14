package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 转发至登录页面
     * @param returnUrl
     * @param model
     * @return
     */
    @GetMapping("toLogin.html")
    public String toLogin(@RequestParam("returnUrl") String returnUrl, Model model){

        // 将登陆之前的地址存在域中 -->> 登录成功之后返回到登录之前的页面
        model.addAttribute("returnUrl", returnUrl);

        return "login";
    }

    /**
     * 登录
     *
     * - 请求方式：post
     * - 请求路径：/login
     * - 请求参数：loginName和password
     * - 返回结果：无
     */
    @PostMapping("login")
    public String login(@RequestParam("loginName")String loginName,
                        @RequestParam("password")String password,
                        @RequestParam("returnUrl")String returnUrl,
                        HttpServletRequest request,
                        HttpServletResponse response
    ){
        this.authService.login(loginName,password,request,response);

        // 重定向回到登录之前的页面
        return "redirect:" + returnUrl;

    }








}
