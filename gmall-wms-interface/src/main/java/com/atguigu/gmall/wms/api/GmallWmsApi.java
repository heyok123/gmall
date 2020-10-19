package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallWmsApi {

    //   验库存 + 锁库存
    @PostMapping("wms/waresku/check/lock")
    public ResponseVo<List<SkuLockVo>> queyAndLock(@RequestBody List<SkuLockVo> skuLockVos);

    //    根据skuid查询库存（gmall-wms中接口已写好）
    @GetMapping("wms/waresku/sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId")Long skuId);
}
