package com.atguigu.gmall.order.service;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String KEY_PREFIXE = "order:token:";

    /**
     * 订单确认页面 + 异步编排
     * @return
     */
    public OrderConfirmVo confirm() {

        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        // 获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 1.查询送货清单 -->> List<UserAddressEntity> addresses;
        CompletableFuture<List<Cart>> cartCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<List<Cart>> checkCartsVo = this.cartClient.queryCheckCarts(userId);
            List<Cart> carts = checkCartsVo.getData();
            if (CollectionUtils.isEmpty(carts)) {
                throw new OrderException("没有选中的购物信息");
            }
            return carts;
        }, threadPoolExecutor);

        CompletableFuture<Void> itemCompletableFuture = cartCompletableFuture.thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setSkuId(cart.getSkuId());
                orderItemVo.setCount(cart.getCount().intValue());

                // 根据skuId查询sku信息
                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    if (skuEntity != null) {
                        orderItemVo.setTitle(skuEntity.getTitle());
                        orderItemVo.setPrice(skuEntity.getPrice());
                        orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                        orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
                    }
                }, threadPoolExecutor);

                // 销售属性
                CompletableFuture<Void> skuAttrValueCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
                    if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                        orderItemVo.setSaleAttrs(skuAttrValueEntities);
                    }
                }, threadPoolExecutor);

                // 营销属性
                CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<ItemSaleVo>> saleResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
                    List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
                    if (!CollectionUtils.isEmpty(itemSaleVos)) {
                        orderItemVo.setSales(itemSaleVos);
                    }
                }, threadPoolExecutor);

                // 库存属性
                CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<WareSkuEntity>> stockResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
                    List<WareSkuEntity> wareSkuEntities = stockResponseVo.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                                wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0);
                    }
                }, threadPoolExecutor);

                // 合并
                CompletableFuture.allOf(skuCompletableFuture,skuAttrValueCompletableFuture,saleCompletableFuture,stockCompletableFuture).join();
                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVo.setItems(orderItemVos);
        }, threadPoolExecutor);

        // 2.收货地址列表 -->> List<UserAddressEntity> addresses;
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<UserAddressEntity>> userAddressResponseVo = this.umsClient.queryAddressByUserId(userId);
            List<UserAddressEntity> addressEntities = userAddressResponseVo.getData();
            if (!CollectionUtils.isEmpty(addressEntities)) {
                orderConfirmVo.setAddresses(addressEntities);
            }
        }, threadPoolExecutor);

        // 3.用户购物积分 -->> Integer bounds;
        CompletableFuture<Void> boundsCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                orderConfirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);

        // 4.防重token -->> String orderToken; --->>> 存入redis中一份
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken);
            this.redisTemplate.opsForValue().set(KEY_PREFIXE + orderToken, orderToken);
        }, threadPoolExecutor);

        // 总体合并
        CompletableFuture.allOf(itemCompletableFuture,addressCompletableFuture,boundsCompletableFuture,tokenCompletableFuture).join();

        return orderConfirmVo;
    }

    /**
     * 1. 验证令牌防止重复提交
     * 2. 验证价格
     * 3. 验证库存，并锁定库存
     * 4. 生成订单
     * 5. 删购物车中对应的记录（消息队列）
     */

    public OrderEntity submitOrder(OrderSubmitVO submitVO) {
        return null;
    }
}
