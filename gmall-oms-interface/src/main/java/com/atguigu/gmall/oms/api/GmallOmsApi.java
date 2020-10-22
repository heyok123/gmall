package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallOmsApi {

    @GetMapping("oms/order/token/{orderSn}")
    public ResponseVo<OrderEntity> queryOrderByOrderSn(@PathVariable("orderSn") String orderSn);

    @PostMapping("oms/order/submit/{userId}")
    public ResponseVo<OrderEntity> saveOrder(@RequestBody OrderSubmitVO orderSubmitVO,
                                             @PathVariable("userId") Long userId);
}
