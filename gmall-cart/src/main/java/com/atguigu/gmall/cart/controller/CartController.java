package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping
public class CartController {

    /**
     * 获取登录用户勾选的购物车
     */
    @GetMapping("check/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckCarts(@PathVariable("userId") Long userId){
        List<Cart> carts = this.cartService.queryCheckCarts(userId);
        return ResponseVo.ok(carts);
    }

    @Autowired
    private CartService cartService;

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId") Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts =  this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    /**
     * 1. 加入购物车
     *
     * - 请求方式：Get
     * - 请求路径：无
     * - 请求参数：?skuId=40&count=2
     *
     * 2. 添加成功，重定向
     *
     * - 请求方式：Get
     * - 请求路径：addCart.html
     * - 请求参数：?skuId=40
     */

    @GetMapping
    public String addCart(Cart cart){
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }
    @GetMapping("addCart.html")
    public String queryCartBySkuId(@RequestParam("skuId") Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";

    }

//    拦截器测试
    @GetMapping("test")
    @ResponseBody
    public String test(){

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);

        return "hello LoginInterceptor";
    }

    @GetMapping("test2")
    @ResponseBody
    public String test2(){

        long start = System.currentTimeMillis();

        System.out.println("test2 method start...");
        this.cartService.executor1();
        this.cartService.executor2();
        System.out.println("test2 method end...");

        System.out.println(System.currentTimeMillis() - start);

        return "hello  Spring Task";
    }

    @GetMapping("test3")
    @ResponseBody
    public String test3(){

        long start = System.currentTimeMillis();

        System.out.println("test3 method start...");
//        this.cartService.executor11();
//        this.cartService.executor22();
        System.out.println("test3 method end...");

        System.out.println(System.currentTimeMillis() - start);

        return "hello  Spring Task  ListenableFuture";
    }


}
