package com.atguigu.gmall.scheduling.handler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CartJobHandler {

    private static final String KEY = "cart:async:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;

    @XxlJob("cartJobHandler")
    public ReturnT<String> executorCart(String params){

        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
        if (listOps.size() == 0){
            // 无异常用户直接返回
            return ReturnT.SUCCESS;
        }
        //获取第一个异常的用户
        String userId = listOps.rightPop();
        while (!StringUtils.isEmpty(userId)){
            // 先删除mysql
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
            // 再查询redis中的Cart
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            // 判断用户购物车是否为空
            if (CollectionUtils.isEmpty(cartJsons)){
                continue; // 为空直接下次循坏判断
            }
            // 不为空 同步到mysql
            cartJsons.forEach(cartJson -> {
                this.cartMapper.insert(JSON.parseObject(cartJson.toString(),Cart.class));
            });
            // 下一个用户
            userId = listOps.rightPop();

        }

        return ReturnT.SUCCESS;
    }

}
