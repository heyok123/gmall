package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.exception.UserException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;

import java.util.Date;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

//    校验数据是否可用 data type
    @Override
    public Boolean checkData(String data, Integer type) {

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();

        switch (type){
            case 1: queryWrapper.eq("username",data); break;
            case 2: queryWrapper.eq("phone",data); break;
            case 3: queryWrapper.eq("email",data); break;
            default: break;
        }

        return this.userMapper.selectCount(queryWrapper) == 0;
    }
//   注册
    @Override
    public void register(UserEntity userEntity, String code) {

        // 1. 查询redis中的验证码，校验验证码
        String phone = userEntity.getPhone();
        if (redisTemplate.opsForValue().get(phone) == null){
            System.out.println("用户信息错误");
            return;
        } else {
            String codeRedis = redisTemplate.opsForValue().get(phone).toString();
            if (!StringUtils.equals(code, codeRedis)){
                System.out.println("短信验证码错误");
                return;
            }
        }
        // 2. 加盐
        String salt = UUID.randomUUID().toString().substring(6);
        userEntity.setSalt(salt);
        // 3. 密码加密
        String md5Password = DigestUtils.md5Hex(salt + userEntity.getPassword());
        userEntity.setPassword(md5Password);
        // 4. 设置注册初始值
        userEntity.setLevelId(1L);
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        // 5.保存
        boolean saveResult = this.save(userEntity);
        // 6. 删除redis中记录
        this.redisTemplate.delete(userEntity.getPhone());


    }

//    查询用户
    @Override
    public UserEntity queryUser(String loginName, String password) {

        // 1. 根据用户名查询用户信息
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName);
        UserEntity user = this.getOne(wrapper);
        if (user == null){
            throw new UserException("用户信息输入有误");
//            return null;
        }
        // 2. 根据查询的用户获取用户密码信息
        String dbPassword = user.getPassword();
        // 3. 将查询的密码进行加密 与 用户输入的密码进行判断
        String md5Password = DigestUtils.md5Hex(dbPassword);
        if (!StringUtils.equals(password, md5Password)){
            return null;
        }

        return user;
    }

}