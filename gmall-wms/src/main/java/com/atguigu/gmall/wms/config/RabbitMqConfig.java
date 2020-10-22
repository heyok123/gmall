package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * （服务器宕机）定时解锁库存 -- 延时队列
     */

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) -> {
            if (!ack){
                log.error("消息没有到达交换机，原因：" + cause);
            }
        });
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            log.error("消息没有到达队列，交换机：{}，路由键：{}，消息内容：{}" + exchange,routingKey,new String(message.getBody()));
        });
    }


    /**
     * 声明延时队列 -- 定时释放库存
     */
    @Bean
    public Queue ttlQueue(){
        // 方式1：
        return QueueBuilder.durable("STOCK_TTL_QUEUE")
                .withArgument("x-message-ttl", 120000)
                .withArgument("x-dead-letter-exchange", "ORDER_EXCHANGE")
                .withArgument("x-dead-letter-routing-key", "stock.unlock").build();
        /**
         * 方式2：
         *  Map<String, Object> arguments = new HashMap<>();
         *         arguments.put("x-message-ttl", 60000);
         *         arguments.put("x-dead-letter-exchange", "order-exchange");
         *         arguments.put("x-dead-letter-routing-key", "order.dead");
         *         return new Queue("order-ttl-queue", true, false, false, arguments);
         */
    }


    /**
     * 延时队列绑定给交换机(ORDER_EXCHANGE)
     */
    @Bean
    public Binding ttlBinding(){
        return new Binding("STOCK_TTL_QUEUE", Binding.DestinationType.QUEUE,"ORDER_EXCHANGE","stock.ttl",null);
    }

























}
