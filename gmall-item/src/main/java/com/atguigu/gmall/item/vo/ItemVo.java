package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 一二三级分类元素 Y2:根据三级分类Id查询一二三级分类
    private List<CategoryEntity> categoryEntities;

    // 品牌 Y3:brandId查询brand
    private Long brandId;
    private String brandName;

    // spu Y4：spuId查询spu
    private Long spuId;
    private String spuName;

    // sku Y1:skuId查询sku信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    // sku图片列表 Y5：skuId查询sku的图片列表
    private List<SkuImagesEntity> images;

    // sku营销信息 Y6：skuId查询sku所有的营销信息（sms）
    private List<ItemSaleVo> sales;

    // sku库存 Y7：skuId查询库存信息
    private Boolean store = false;

    // spu下的所有sku营销属性信息 Y8:根据spuId查询spu下的所有销售属性
    // （attrId:8,attrName:'颜色'，attrValues:{'白色'，'金色'}）,
    // （attrId:9,attrName:'内存'，attrValues:{'8G'，'12G'}）,
    // （attrId:10,attrName:'存储'，attrValues:{'128g'，'256g'}）,
    private List<SaleAttrValueVo> saleAttrs;

    // 获取当前sku的销售属性 Y9：根据skuId查询sku的销售属性
    // {8:'白色',9:'8G',10:'128g'}
    private Map<Long,String> saleAttr;

    // 销售属性组合和skuid的映射关系 Y10：根据spuId查询spu下所有sku的销售属性组合和skuId的映射关系
    // {'白色,8G,128g':10,'金色,12G,256g':11}
    private String skuJsons;

    // 商品描述信息 Y11 :spuid查询
    private List<String> spuImages;

    // 商品详情页 Y12: categoryId/spuId/skuid
    private List<ItemGroupVo> groups;





























}
