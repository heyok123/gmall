package com.atguigu.gmall.mms.service;


import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;


import java.util.HashMap;

@Service
public class MmsService {


    /**
     * 1. 发送短信验证码
     */
    public boolean send(String phone, HashMap<String, Object> map) {
        // 1. 获取初始化对象
        DefaultProfile profile =
                DefaultProfile.getProfile("default", "LTAI4Fxzu66NkxYeeQ7XDzyd", "0ehfexOzTC4dnDQgHcQtb1kFfTHKkb");
        IAcsClient client = new DefaultAcsClient(profile);

        // 2. 创建request对象
        CommonRequest request = new CommonRequest();

        // 3. request设置参数
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");

        Gson gson = new Gson();

        request.putQueryParameter("PhoneNumbers", phone);
        request.putQueryParameter("SignName", "谷粒商城");
        request.putQueryParameter("TemplateCode", "MMS_201480228");
//        request.putQueryParameter("TemplateParam", Json.toJSONString(map));
        request.putQueryParameter("TemplateParam", gson.toJson(map));

        // 4. 获取response对象
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());

            // 5. 获取最终结果
            return response.getHttpResponse().isSuccess();
        }  catch (ClientException e) {
            e.printStackTrace();
        }
        return false;
    }
}
