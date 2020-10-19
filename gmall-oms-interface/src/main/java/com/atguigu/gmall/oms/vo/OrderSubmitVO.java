package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVO {

    private String orderToken;
    private BigDecimal totalPrice;
    private UserAddressEntity address;
    private Integer payType;
    private String deliveryCompany; // 配送方式
    private List<OrderItemVo> items; // 订单详情
    private Integer bounds; // 积分

}
