package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.Comments;

@Mapper
public interface CommentsMapper extends BaseMapper<Comments> {
}
