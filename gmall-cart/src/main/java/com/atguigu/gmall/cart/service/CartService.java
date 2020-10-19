package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private CartMapper cartMapper;

    //  获取登录用户勾选的购物车
    public List<Cart> queryCheckCarts(Long userId) {

        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 获取购物车
        List<Object> cartJsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartJsons)){
            return  null;
        }
        // 获取选中的购物车
        return cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class))
                .filter(cart -> cart.getCheck()).collect(Collectors.toList());

    }

    private String getUserId(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if(userInfo.getUserId() != null){
            // 有id 已经登录 -->> 添加cart以userId作为key
            return userInfo.getUserId().toString();
        }
        // 否则以userKey来添加cart
        return userInfo.getUserKey();
    }

    public void addCart(Cart cart) {
        // 获取登录信息
        String userId = cart.getUserId();
        // 获取外层map的key  ---  (hash结构)
        String key = KEY_PREFIX + userId;
        // 通过userId/userKey来获取购物车 即内层map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 获取skuId以及count
        String skuIdStr = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        // 判断购物车中是否已加入过这件sku -- 加过则更新数量
        if (hashOps.hasKey(skuIdStr)){
            String skuJson = hashOps.get(skuIdStr).toString();
             cart = JSON.parseObject(skuJson, Cart.class);
             cart.setCount(cart.getCount().add(count));
             // 将数据更新到mySql
//            this.cartMapper.update(cart, new UpdateWrapper<Cart>()
//                    .eq("user_id", cart.getUserId()).eq("sku_id", cart.getSkuId()));
            this.cartAsyncService.updateCart(userId,cart);
        } else {
            // 之前购物车没有此sku, 则新增一条记录cart：skuId/count
            cart.setUserId(userId);
            // >>><<< 根据skuId来获取sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null){
                cart.setTitle(skuEntity.getTitle());
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setPrice(skuEntity.getPrice());
            }

            // >>><<< 根据skuId获取销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
            // String saleAttrs; 销售属性：List<SkuAttrValueEntity>的json格式
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            // >>><<< 根据skuId获取营销属性
            ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemSaleResponseVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)){
                // String sales; 营销信息: List<ItemSaleVo>的json格式
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }

            // >>><<< 根据skuId获取库存信息
            ResponseVo<List<WareSkuEntity>> skuWareResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = skuWareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                boolean ware = wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                    wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0
                );
                cart.setStore(ware);
            }
            // 选中状态 check:默认true
            cart.setCheck(true);
            // 新增cart
//            this.cartMapper.insert(cart);
            this.cartAsyncService.insertCart(userId,cart);
            // 将价格进行缓存redis
            if (skuEntity != null){
                this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuIdStr, skuEntity.getPrice().toString());
            }
        }

        // 将数据存入redis中
        hashOps.put(skuIdStr,JSON.toJSONString(cart));



    }

    public Cart queryCartBySkuId(Long skuId) {

        // 获取用户登录id/key信息
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        // 获取redis中以此key作为键的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(key).toString();
            return JSON.parseObject(cartJson,Cart.class);
        }
        // 没有此cart则 抛出异常
        throw new RuntimeException("你的购物车没有此商品！");

    }

    public List<Cart> queryCarts() {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();

        // 未登录 carts
        String unLoginkey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> unHashOps = this.redisTemplate.boundHashOps(unLoginkey);
        List<Object> cartJsons = unHashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(cartJsons)) {
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询实时价格
                String priceCurrentStr = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(priceCurrentStr));
                return cart;
            }).collect(Collectors.toList());
        }

        // 获取用户id 判断是否登录
        Long userId = userInfo.getUserId();
        if (userId == null){
            // 未登录
            return unLoginCarts;
        }

        // 判断未登录是否有cart 有则合并
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                if (loginHashOps.hasKey(cart.getSkuId().toString())){
                    // 登录情况下也有同样的sku -- 更新count
                    // 获取未登录是的sku count
                    BigDecimal unLoginCount = cart.getCount();

                    // 获取登录情况下的数据
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                     cart = JSON.parseObject(cartJson, Cart.class);
                     // 合并count
                     cart.setCount(cart.getCount().add(unLoginCount));
                     // 存入  mysql
                    this.cartAsyncService.updateCart(userId.toString(),cart);
                } else {
                    // 已经登录情况没有此sku -- 直接新增一条sku
                    cart.setUserId(userId.toString());
                    cartAsyncService.insertCart(userId.toString(),cart);
                }
                // 存入redis (转json)
                loginHashOps.put(cart.getSkuId(),JSON.toJSONString(cart));
            });
        }
        // 合并成功后 删除未登录的购物车数据
        this.redisTemplate.delete(unLoginkey);
        this.cartAsyncService.deleteCartByUserId(userKey);

        // 根据userId获取登录状态的购物车
        List<Object> loginCarts = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCarts)) {
            return loginCarts.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 价格缓存redis
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        // 获取用户的登录信息
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            // 获取要修改的count
            BigDecimal count = cart.getCount();

            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            // 修改count
            cart.setCount(count);

            // 写回redis  数据库
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            this.cartAsyncService.updateCart(userId,cart);
        }


    }

    /**
     * 删除cart
     * @param skuId
     */
    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            // 删除redis
            hashOps.delete(skuId.toString());
            // 异步删除mysql
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId,skuId);
        }
    }

//    @Async
//    public ListenableFuture<String> executor11() {
//        try {
//            System.out.println("executor11方法开始执行");
//            Thread.sleep(3000);
//            System.out.println("executor11方法结束执行");
//            // 正常返回结果集
//            return AsyncResult.forValue("heyok executor11");
//        } catch (InterruptedException e) {
//            // 异常返回
//            e.printStackTrace();
//            return AsyncResult.forExecutionException(e);
//        }
//    }

//    @Async
//    public ListenableFuture<String> executor22() {
//        try {
//            System.out.println("executor22方法开始执行");
//            Thread.sleep(4000);
//            System.out.println("executor22方法结束执行");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//          return "heyok executor22";
//    }

    @Async
    public String executor1() {
        try {
            System.out.println("executor1方法开始执行");
            Thread.sleep(3000);
            System.out.println("executor1方法结束执行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "heyok executor1";

    }

    @Async
    public String executor2() {
        try {
            System.out.println("executor2方法开始执行");
            Thread.sleep(4000);
            System.out.println("executor2方法结束执行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "heyok executor2";
    }


}
