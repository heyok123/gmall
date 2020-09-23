package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "sms-service")
public interface GmallSmsClient extends GmallSmsApi {
//    @PostMapping("sms/skubounds/skusale/save")
//    public ResponseVo saveSkuSaleInfo(@RequestBody SkuSaleVo skuSaleVo);

}
