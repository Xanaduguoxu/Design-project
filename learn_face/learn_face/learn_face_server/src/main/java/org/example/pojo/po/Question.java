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
public class Question extends Base {

    private String question;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> choice;

    private String answer;

    private String parse;

    private String category;

    private Integer score;

    private String type;
}
