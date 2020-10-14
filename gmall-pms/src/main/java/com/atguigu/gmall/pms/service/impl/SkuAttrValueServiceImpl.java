package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

//    根据skuId查询检索属性及值
    @Override
    public List<SkuAttrValueEntity> querySearchSkuAttrValueByCidAndSkuid(Long cid, Long skuid) {

        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)) {
            return null;
        }

        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuid).in("attr_id", attrIds));

    }

//    根据spuId查询spu下的所有销售属性
    @Override
    public List<SaleAttrValueVo> querySkuAttrValuesBySpuId(Long spuId) {

        List<SkuAttrValueEntity> skuAttrValueEntities = this.attrValueMapper.querySkuAttrValuesBySpuId(spuId);
        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
            // 根据attrId进行分组
            Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));

            ArrayList<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();

            map.forEach((attrId,attrValueEntities) -> {
                SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
                saleAttrValueVo.setAttrId(attrId);
                saleAttrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
                saleAttrValueVo.setAttrValues(attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
                saleAttrValueVos.add(saleAttrValueVo);
            });

            return saleAttrValueVos;
        }
        return null;
    }

//  销售属性组合和skuid的映射关系 Y10：根据spuId查询spu下所有sku的销售属性组合和skuId的映射关系
    @Override
    public String querySkuIdMappingSaleAttrValueBySpuId(Long spuId) {
        List<Map<String, Object>> maps = this.attrValueMapper.querySkuIdMappingSaleAttrValueBySpuId(spuId);

        if (CollectionUtils.isEmpty(maps)) {
            return null;
        }
        // {'白色,8G,128g' : 10,   '金色,12G,256g' : 11}   -->>  key--value...
        Map<String, Long> collect = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long)map.get("sku_id")));
        return JSON.toJSONString(collect);
    }

}