package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsFeignClient;
import com.atguigu.gmall.search.feign.GmallWmsFeignClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private GmallWmsFeignClient wmsFeignClient;

    @Test
    void contextLoads() {

        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);
    }

   /* @Test
    void importData(){
        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            // 分批查询spu
            PageParamVo pageParamVo = new PageParamVo();
            pageParamVo.setPageSize(pageSize);
            pageParamVo.setPageNum(pageNum);
            ResponseVo<List<SpuEntity>> listResponseVo = this.pmsFeignClient.querSpuByCidPage(pageParamVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();

            if (CollectionUtils.isEmpty(spuEntities)) {
                continue;
            }

            // 遍历spu -》 sku -》 goods
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuEntityResponseVo = this.pmsFeignClient.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuEntityResponseVo.getData();

                if (!CollectionUtils.isEmpty(skuEntities)) {
                    // sku -> goods
                    skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        // sku商品列表所需字段
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setPrice(skuEntity.getPrice().doubleValue());

                        // 品牌聚合所需字段
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsFeignClient.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        // 分类聚合所需字段
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsFeignClient.queryCategoryById(skuEntity.getCatagoryId());



                        return null;
                    }).collect(Collectors.toList());

                }

            });


            pageNum++;


        }while (pageSize == 100);


    }
*/
    @Test
    void contextData(){

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            // 分批查询spu
            PageParamVo pageParamVo = new PageParamVo();
            pageParamVo.setPageNum(pageNum);
            pageParamVo.setPageSize(pageSize);

            ResponseVo<List<SpuEntity>> listResponseVo = this.pmsFeignClient.querSpuByCidPage(pageParamVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();

            if (CollectionUtils.isEmpty(spuEntities)) {
                continue;
            }

            // 遍历当前页的spu 查询sku 并且转化为goods对象集合
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skusResponseVo = this.pmsFeignClient.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skusResponseVo.getData();

                if (!CollectionUtils.isEmpty(skuEntities)){
                    // 转化为goods对象
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        // sku相关信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setPrice(skuEntity.getPrice().doubleValue());

                        // 品牌相关信息
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsFeignClient.queryBrandById(spuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        // 分类相关信息
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsFeignClient.queryCategoryById(spuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        // 创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());

                        // 库存相关信息
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsFeignClient.queryWareSkuBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());

                        }

                        ArrayList<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();

                        // 规格参数相关信息 skuAttrValueEntity
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsFeignClient.querySearchSkuAttrValueByCidAndSkuid(skuEntity.getCatagoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                            searchAttrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));

                        }

                        // 规格参数相关信息 spuAttrValueEntity
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = this.pmsFeignClient.querySearchSpuAttrValuesByCidAndSpuId(skuEntity.getCatagoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVo.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                            searchAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));

                        }

                        goods.setSearchAttrs(searchAttrValueVos);
                        return goods;

                    }).collect(Collectors.toList());

                    // 批量导入到es中
                    this.goodsRepository.saveAll(goodsList);
                }
            });

            pageSize = spuEntities.size();
            pageNum++;

        } while (pageSize == 100);

    }

}
