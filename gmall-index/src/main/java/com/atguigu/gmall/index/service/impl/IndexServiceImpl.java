package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.cache.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cate:";

    @Override
    public List<CategoryEntity> queryLvl1Categories() {

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(0L);
        return listResponseVo.getData();

    }

    @Override
    @GmallCache(prefix = KEY_PREFIX,lock = "lock:",timeout = 129600,random = 7200)
    public List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid) {

        // 缓存中没有 则先查询 远程调用
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();


        return categoryEntities;
    }



//    获取二三级分类
//    @Override
    public List<CategoryEntity> queryLvl2CategoriesWithSub2(Long pid) {

        // 查询缓存，命中直接返回 ：一级分类id作为key 返回数据作为value
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)){
            JSON.parseArray(json,CategoryEntity.class);
        }

        // 缓存中没有 则先查询 远程调用
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        if (CollectionUtils.isEmpty(categoryEntities)){
            // 缓存穿透:防止恶意大量访问不存在的数据 判断/key--value  -->>  key--null
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryEntities),5, TimeUnit.MINUTES);
        }else {
            // 缓存雪崩:防止大量数据同一时间过期
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities),60 + new Random().nextInt(5),TimeUnit.DAYS);
        }
        return categoryEntities;
    }

    @Override
    public void testLock() {

        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        // 获取锁成功，执行业务逻辑
        String countString = this.redisTemplate.opsForValue().get("count");
        if (StringUtils.isBlank(countString)) {
            this.redisTemplate.opsForValue().set("count", "1");
        }
        int count = Integer.parseInt(countString);
        this.redisTemplate.opsForValue().set("count", String.valueOf(++count));

        lock.unlock();

    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);

        System.out.println("==============");


        //rwLock.writeLock().unlock();
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);

        System.out.println("==============");
    }

    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);

        latch.await();
        System.out.println("班长要锁门");
        return "班长锁门成功";
    }

    public String testCountDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();

        return "出来了一位同学";
    }
}
