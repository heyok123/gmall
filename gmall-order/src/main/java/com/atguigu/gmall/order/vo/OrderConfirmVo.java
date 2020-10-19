package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    // 收货地址列表
    private List<UserAddressEntity> addresses;
    // 送货清单
    private List<OrderItemVo> items;
    // 购物积分
    private Integer bounds;
    // 防重标识token
    private String orderToken;
}
