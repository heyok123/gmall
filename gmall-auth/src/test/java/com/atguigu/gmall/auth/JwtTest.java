package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\java\\test\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\java\\test\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDI2NTE1Nzh9.ORIvZ8PCfyPpFxSlf08tkqyWoOOcsUUyLT8SXOBoWWZuHiiH6f01I00vULHZzjmJjmnXxncp1HmkynbReE2ZKaahdW9xeiX64Wgin9VNknCLF-Ws2mYzoL-lW01XR83PuJ-fBDw-IcZv2iLv-6sH9xDgk1eVTgCwL6pyhriD0po7PIOA9mVjZTIO9RhI__91ovMBO2yZ9RGiyH-yJ_hCd20xOJ7fa_xc7LODsVmgtzgScEFltnCtiXkszt_r1eHlQmrNcDR2nmXAYk35pw_Ny-bj4FXVQt4KuhRWXf7z0K-SgtE31-0DQfxiDQ1saAZONATptEJaDA8lW6k5imsq8g";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}