package com.atguigu.gmall.wms;

import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;
    private Boolean lock; //锁定状态
    private Long wareSkuId;//锁定仓库的id
    private String orderToken;

}
