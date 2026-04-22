package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.Question;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
