package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author zws
 * @email zws@atguigu.com
 * @date 2020-10-13 15:26:02
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
