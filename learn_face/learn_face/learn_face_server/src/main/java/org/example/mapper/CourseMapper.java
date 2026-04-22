package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.Course;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
