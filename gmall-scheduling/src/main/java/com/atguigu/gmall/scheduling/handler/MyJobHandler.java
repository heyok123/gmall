package com.atguigu.gmall.scheduling.handler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    @XxlJob("heyokJobHandler")
    public ReturnT<String> excute(String param){
        XxlJobLogger.log("日志：day day up");
        System.out.println("任务执行：" + param + "\t线程：" + Thread.currentThread().getName());
        return ReturnT.SUCCESS;
    }

}
