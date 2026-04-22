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
public class Comments extends Base {

    private Long correlationId;

    private Long parentId;

    private String sender;

    private String senderAvatar;

    private String receiver;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String belong;
}
