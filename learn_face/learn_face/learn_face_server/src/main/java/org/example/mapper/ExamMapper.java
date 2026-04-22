
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.Exam;

@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}