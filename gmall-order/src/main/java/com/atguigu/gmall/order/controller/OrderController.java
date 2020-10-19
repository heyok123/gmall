package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 提交订单返回订单id
     */
    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<Object> submitOrder(@RequestBody OrderSubmitVO submitVO){
        OrderEntity orderEntity = this.orderService.submitOrder(submitVO);
        return ResponseVo.ok(orderEntity.getDeliverySn());

    }

    // 完成订单结算页数据查询接口
    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo orderConfirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", orderConfirmVo);
        return "trade";
    }
    /*@GetMapping("confirm")
    public ResponseVo<OrderConfirmVo> confirm(){
        OrderConfirmVo orderConfirmVo = this.orderService.confirm();
        return ResponseVo.ok(orderConfirmVo);
    }*/


}
