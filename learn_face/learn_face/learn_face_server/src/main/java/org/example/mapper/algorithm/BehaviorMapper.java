package org.example.mapper.algorithm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.pojo.po.algorithm.Action;

import java.util.List;

@Mapper
public interface BehaviorMapper extends BaseMapper<Action> {

    @Select("${sql}")
    List<Action> executeCustomQuery(@Param("sql") String sql);
}
