package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author zws
 * @email zws@atguigu.com
 * @date 2020-09-22 14:31:22
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

    List<WareSkuEntity> queryStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    int lockStock(@Param("id") Long id, @Param("count") Integer count);
    int unLockStock(@Param("id") Long id, @Param("count") Integer count);
}
