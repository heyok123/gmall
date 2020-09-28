package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallPmsApi {

//  分页查询已上架的SPU信息
    @PostMapping("pms/spu/page")
    public ResponseVo<List<SpuEntity>> querSpuByCidPage(@RequestBody PageParamVo pageParamVo);

//  根据SpuId查询对应的SKU信息（接口已写好）
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId")Long spuId);

//  根据分类id查询商品分类（逆向工程已自动生成）
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

//  根据品牌id查询品牌（逆向工程已自动生成）
    @GetMapping("/pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

//  根据spuId查询检索规格参数及值
    @PostMapping("pms/spuattrvalue/search/{cid}/{spuid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValuesByCidAndSpuId(@PathVariable("cid") Long cid,
                                                                                      @PathVariable("spuid") Long spuid);

//  根据skuId查询检索规格参数及值
    @GetMapping("pms/skuattrvalue/search/{cid}/{skuid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValueByCidAndSkuid(@PathVariable("cid") Long cid,
                                                                                     @PathVariable("skuid") Long skuid);



}
