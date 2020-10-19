package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;

import java.util.List;

/**
 * 商品库存
 *
 * @author zws
 * @email zws@atguigu.com
 * @date 2020-09-22 14:31:22
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuLockVo> queryAndLock(List<SkuLockVo> skuLockVos);
}

