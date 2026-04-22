package org.example.pojo.po;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.pojo.base.Base;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class Course extends Base {

    private String name;

    private String category;

    private String brief;

    private String image;

    private String courseware;

    private String duration;

    private Integer views;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<JSONObject> chapters;
}
