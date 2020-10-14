package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategory(@PathVariable("parentId")Long parentId);

    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSub(@PathVariable("pid") Long pid);

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

//    spuId查询spu
    @GetMapping("pms/spu/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

//    skuId查询sku信息
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

//    根据三级分类Id查询一二三级分类
    @GetMapping("pms/category/all/{cid3}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByCid3(@PathVariable("cid3")Long cid3);

//    skuId查询sku的图片列表
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId") Long skuId);

//    商品描述信息
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

//    根据spuId查询spu下的所有销售属性
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySkuAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

//    根据skuId查询sku的销售属性
    @GetMapping("pms/skuattrvalue/sku/skuId")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueBySkuId(@PathVariable("skuId") Long skuId);

//    销售属性组合和skuid的映射关系 Y10：根据spuId查询spu下所有sku的销售属性组合和skuId的映射关系
    @GetMapping("pms/skuattrvalue/spu/mapping/{spuId}")
    public ResponseVo<String> querySkuIdMappingSaleAttrValueBySpuId(@PathVariable("spuId") Long spuId);

//    商品详情页 Y12: categoryId/spuId/skuid
    @GetMapping("pms/attrgroup/withattrvalues/{categoryId}")
    public ResponseVo<List<ItemGroupVo>> queryGroupsWithAttrAndValueByCidAndSpuIdAndSkuId(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam("spuId") Long spuId,
            @RequestParam("skuId") Long skuId
    );


}
