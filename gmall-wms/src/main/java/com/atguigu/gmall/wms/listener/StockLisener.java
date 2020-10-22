package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * 释放库存（调用超时，网络抖动...造成订单无效）
 */

@Component
public class StockLisener {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "stock:lock:";


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK.UNLOCK.QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unLock(String orderToken, Channel channel, Message message) throws IOException {
        
        // 解析orderToken 获取订单号
        String orderTokenStr = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        // 如果订单中的商品库存信息为空 则不作处理
        if (StringUtils.isEmpty(orderTokenStr)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        // 有库存信息  -- 反序列化
        List<SkuLockVo> skuLockVoList = JSON.parseArray(orderToken, SkuLockVo.class);
        // 判断商品的锁定库存信息是否为空
        if (CollectionUtils.isEmpty(skuLockVoList)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        // 遍历sku 解锁库存
        skuLockVoList.forEach(skuLockVo -> {
            this.wareSkuMapper.unLockStock(skuLockVo.getSkuId(), skuLockVo.getCount());
        });

        // 删除redis中的库存锁定信息 防止重复解锁库存
        this.redisTemplate.delete(KEY_PREFIX + orderToken);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);


    }


}
