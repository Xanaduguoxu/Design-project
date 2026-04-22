package org.example.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ai.po.Chat;

@Mapper
public interface ChatMapper extends BaseMapper<Chat> {
}
