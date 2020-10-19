package com.atguigu.gmall.cart.listener;

import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.List;

@Component
public class CartListener {

    private static final String PRICE_PREFIX = "cart:price:";

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_SPU_QUEUE",durable = "true"),
            exchange = @Exchange(value = "CART_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }

        ResponseVo<List<SkuEntity>> skutResponseVo = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skuEntities = skutResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)) {
            skuEntities.forEach(skuEntity -> {
                // 获取旧的价格
                String price = this.redisTemplate.opsForValue().get(PRICE_PREFIX + skuEntity.getId());
                if (StringUtils.isNotBlank(price)){
                    // 保存新价格
                    this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(),skuEntity.getPrice().toString());
                }
            });
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

        }


    }

}
