package org.example.pojo.po;

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
public class Forum extends Base {

    private String author;

    private String avatar;

    private String bio;

    private String title;

    private String content;

    private Integer likes;

    private Integer views;

    private String image;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageArr;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
}
