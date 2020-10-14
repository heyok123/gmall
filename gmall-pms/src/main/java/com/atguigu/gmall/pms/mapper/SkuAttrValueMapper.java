package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author zws
 * @email zws@atguigu.com
 * @date 2020-09-21 12:54:22
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {


    List<SkuAttrValueEntity> querySkuAttrValuesBySpuId(Long spuId);

    List<Map<String, Object>> querySkuIdMappingSaleAttrValueBySpuId(Long spuId);
}
