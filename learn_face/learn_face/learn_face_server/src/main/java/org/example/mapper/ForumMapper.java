
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.po.Forum;

@Mapper
public interface ForumMapper extends BaseMapper<Forum> {
}