package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author zws
 * @email zws@atguigu.com
 * @date 2020-09-21 15:24:41
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

