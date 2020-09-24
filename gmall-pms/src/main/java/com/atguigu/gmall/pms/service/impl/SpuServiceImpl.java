package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private SpuDescService spuDescService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

//    spu商品信息查询
    @Override
    public PageResultVo querySpuInfo(PageParamVo pageParamVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        // 1.分类id不为0，说明要查本类，如果为0，说明查询全站
        if (categoryId != 0) {
            wrapper.eq("category_id", categoryId);
        }
        // 2.查询关键字
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key).or().like("name", key));
        }
        // 3.使用mp的分页查询方法
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @Override
//    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional // 分布式事务
    public void bigSave(SpuVo spuVo) {
        // 1. 保存spu
        // 1.1 保存spu基本信息spu_info
        Long spuId = saveSpu(spuVo);

        // 1.2 保存spu描述信息spu_info_desc
//        saveSpuDesc(spuVo, spuId);
        this.spuDescService.saveSpuDesc(spuVo, spuId);

        // 1.3 保存spu规格参数信息：
        // baseAttrs: [{attrId: 1, attrName: "上市年份", valueSelected: ["2020"]},…]
        saveSpuBaseAttr(spuVo, spuId);

        // 2. 保存sku
        // skus: [{attr_3: "白色", name_3: "机身颜色", price: "5999", stock: 0, growBounds: "122",
        //       buyBounds: "222",…},"saleAttrs": [{"attrId": "3","attrValue": "黑色"]...}]
        saveSku(spuVo, spuId);
//        int i = 1/0;
    }

    /**
     * 保存spu基本信息spu_info
     * @param spuVo
     * @return
     */
    public Long saveSpu(SpuVo spuVo) {
        spuVo.setPublishStatus(1);
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        return spuVo.getId();
    }

    /**
     * 保存spu描述信息spu_info_desc
     * @param spuVo
     * @param spuId
     */
    public void saveSpuDesc(SpuVo spuVo, Long spuId) {
        if (!CollectionUtils.isEmpty(spuVo.getSpuImages())) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuVo.getSpuImages(),","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }

    /**
     * 保存spu规格参数信息
     * @param spuVo
     * @param spuId
     */
    public void saveSpuBaseAttr(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)){
            this.spuAttrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(0);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }

    /**
     * 保存sku
     * @param spuVo
     * @param spuId
     */
    public void saveSku(SpuVo spuVo, Long spuId) {
        List<SkuVo> skuVos = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skuVos)) {
            return;
        }
        skuVos.forEach(skuVo -> {
            // 2.1 保存sku基本信息
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(skuVo,skuEntity);
            skuEntity.setBrandId(skuVo.getBrandId());
            skuEntity.setCatagoryId(spuVo.getCategoryId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                // 设置第一张图片为默认图片
                skuEntity.setDefaultImage(skuEntity.getDefaultImage() == null ? images.get(0) : skuEntity.getDefaultImage());
            }
            skuEntity.setSpuId(spuId);
            this.skuMapper.insert(skuEntity);
            // 2.2 保存sku图片信息
            Long skuId = skuEntity.getId();
            if (!CollectionUtils.isEmpty(images)) {
                String defaultImages = images.get(0);
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(defaultImages, image) ? 1 : 0);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setUrl(image);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(skuImagesEntities);
            }

            // 2.3 保存sku销售信息（销售属性）
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(saleAttr -> {
                    saleAttr.setSort(0);
                    saleAttr.setSkuId(skuId);
                });
            }
            this.skuAttrValueService.saveBatch(saleAttrs);

            // 3. 保存营销相关信息（远程调用gmall-sms）
            // 3.1 积分优惠
            // 3.2 满减优惠
            // 3.3 折扣优惠
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSkuSaleInfo(skuSaleVo);

        });
    }



}