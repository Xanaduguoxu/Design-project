package org.example.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.common.Auth;

@Mapper
public interface AuthMapper extends BaseMapper<Auth> {
}
