package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

/**
 * 获取公钥
 */

@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    // 公钥
    private String pubKeyPath;
    private PublicKey publicKey;
    // cookie
    private String cookieName;

    @PostConstruct
    public void init(){

        try {
            // 获取公钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("获取公钥失败:" + e);
            e.printStackTrace();
            throw new RuntimeException();
        }

    }



}
