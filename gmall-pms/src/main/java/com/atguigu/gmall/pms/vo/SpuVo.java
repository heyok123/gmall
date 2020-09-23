package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuVo extends SpuEntity {

    /**
     * SpuEntity基础封装三个属性：spuImages/baseAttrs/skus
     */

    // 图片信息
    private List<String> spuImages;

    // 基本属性信息
    private List<SpuAttrValueVo> baseAttrs;

    // sku信息
    private List<SkuVo> skus;

/**
 * spuImages: [0: "https://guli-file-ws.oss-cn-shanghai.aliyuncs.com/2020-09-22/a26d2230-4049-4c29-860f-379e6e492eb6_10.jpg"]
 *
 * baseAttrs: [{attrId: 1, attrName: "上市年份", valueSelected: ["2020"]},…]
 *
 * skus: [{attr_3: "白色", name_3: "机身颜色", price: "5999", stock: 0, growBounds: "122", buyBounds: "222",…},…]
 */

}
