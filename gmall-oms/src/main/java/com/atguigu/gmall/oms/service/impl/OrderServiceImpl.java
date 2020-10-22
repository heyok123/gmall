package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVO orderSubmitVO, Long userId) {

        // 1.保存订单表
        OrderEntity orderEntity = new OrderEntity();

        // 查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null){
            orderEntity.setUserId(userEntity.getId());
            orderEntity.setUsername(userEntity.getUsername());
        }
        orderEntity.setOrderSn(orderSubmitVO.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(orderSubmitVO.getTotalPrice());
        orderEntity.setPayType(orderEntity.getPayType());
        orderEntity.setSourceType(1);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(orderSubmitVO.getDeliveryCompany());

        // 收货地址
        UserAddressEntity address = orderSubmitVO.getAddress();
        if (address != null){
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
        }

        orderEntity.setConfirmStatus(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(orderSubmitVO.getBounds());

        this.save(orderEntity);
        Long id = orderEntity.getId();

        // 2. 保存订单详情表
        List<OrderItemVo> items = orderSubmitVO.getItems();
        if (!CollectionUtils.isEmpty(items)) {
            this.orderItemService.saveBatch(
                    items.stream().map(item -> {
                        OrderItemEntity orderItemEntity = new OrderItemEntity();
                        orderItemEntity.setOrderId(id);
                        orderItemEntity.setOrderSn(orderSubmitVO.getOrderToken());

                        // 根据skuId查询sku
                        ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
                        SkuEntity skuEntity = skuEntityResponseVo.getData();
                        if (skuEntity != null){
                            orderItemEntity.setSkuId(skuEntity.getId());
                            orderItemEntity.setSkuName(skuEntity.getName());
                            orderItemEntity.setSkuQuantity(item.getCount().intValue());
                            orderItemEntity.setSkuPrice(skuEntity.getPrice());
                            orderItemEntity.setSkuPic(skuEntity.getDefaultImage());

                            // 销售属性
                            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySkuAttrValueBySkuId(item.getSkuId());
                            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
                            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
                            orderItemEntity.setCategoryId(skuEntity.getCatagoryId());

                            // 品牌
                            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                            BrandEntity brandEntity = brandEntityResponseVo.getData();
                            if (brandEntity != null){
                                orderItemEntity.setSpuBrand(brandEntity.getName());
                            }

                            //spu
                            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                            SpuEntity spuEntity = spuEntityResponseVo.getData();
                            if (spuEntity != null){
                                orderItemEntity.setSpuId(spuEntity.getId());
                                orderItemEntity.setSpuName(spuEntity.getName());
                            }

                            // 描述
                            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
                            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                            if (spuDescEntity != null){
                                orderItemEntity.setSpuPic(spuDescEntity.getDecript());
                            }

                        }

                        return orderItemEntity;
                    }).collect(Collectors.toList())
            );
        }

        // 订单创建后 发送延时消息给消息队列mq --- 定时关单
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", orderSubmitVO.getOrderToken());


        return orderEntity;
    }

}