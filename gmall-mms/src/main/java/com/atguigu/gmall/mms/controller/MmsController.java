package com.atguigu.gmall.mms.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.mms.service.MmsService;
import com.atguigu.gmall.mms.utils.RandomUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Api(value = "短信发送")
@RestController
@CrossOrigin
@RequestMapping("/msm/send")
public class MmsController {

    @Autowired
    private MmsService mmsService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @ApiOperation("发送短信验证码")
    @GetMapping("sendMsmPhone/{phone}")
    public ResponseVo sendMsmPhone(@PathVariable String phone){
        // 1. 根据手机号查询redis：是否有相关验证码
        String codeMessage = redisTemplate.opsForValue().get(phone);
        if (!StringUtils.isEmpty(codeMessage)){
            return ResponseVo.ok();
        }

        // 2. 生成验证码
        String code = RandomUtil.getFourBitRandom();
        HashMap<String, Object> map = new HashMap<>();
        map.put("code",code);

        // 3. 调用接口 发送短信验证码
        boolean isSend = mmsService.send(phone,map);

        // 4. 验证发送成功 验证码存入redis
        if (isSend){
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return ResponseVo.ok();
        } else {
            return ResponseVo.ok("短信发送失败");
        }


    }

}
