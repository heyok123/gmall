package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {


    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    //  验库存 + 锁库存
    @Transactional
    @Override
    public List<SkuLockVo> queryAndLock(List<SkuLockVo> skuLockVos) {

        if (CollectionUtils.isEmpty(skuLockVos)) {
            return null;
        }

        // 将每个sku进行锁库存
        skuLockVos.forEach(skuLockVo -> {
          this.checkLock(skuLockVo); // 验库存+锁库存
        });

        // 一个锁定失败，其他全部解锁
        List<SkuLockVo> successLock = skuLockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
        List<SkuLockVo> errorLock = skuLockVos.stream().filter(skuLockVo -> !skuLockVo.getLock()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(errorLock)){
            // 有锁定库存失败，需要解锁全部(减去锁定的库存)
            successLock.forEach(skuLockVo -> {
                this.wareSkuMapper.unLockStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });
            return skuLockVos;
        }
        // 将锁定库存的信息保存到redis中 用来以后解锁库存
        String orderToken = skuLockVos.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVos));

        // ***为了防止锁库存成功之后 服务器宕机 而没有下单 造成库存锁死 需要发送消息给mq(延时队列) 定时释放库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);


        // 锁定成功 无需展示
        return null;
    }

    /**
     * 锁库存 ： （验库存+锁库存同一个方法，以保证原子性）
     */
    private void checkLock(SkuLockVo skuLockVo){
        RLock lock = this.redissonClient.getFairLock("stock:" + skuLockVo.getSkuId());
        // 分布式锁 ： 防止多人同时锁库存
        lock.lock();
        // 验库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.queryStock(skuLockVo.getSkuId(),skuLockVo.getCount());

        // 库存不足
        if (CollectionUtils.isEmpty(wareSkuEntities)){
            skuLockVo.setLock(false); // 库存不足
            lock.unlock();  // 释放锁
            return;
        }

        // 锁库存
        if (this.wareSkuMapper.lockStock(wareSkuEntities.get(0).getId(),skuLockVo.getCount()) == 1){
            skuLockVo.setLock(true); // 锁库存
            skuLockVo.setWareSkuId(wareSkuEntities.get(0).getId());
        }else {
            skuLockVo.setLock(false);
        }
        lock.unlock(); // 释放分布式锁

    }


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }


}