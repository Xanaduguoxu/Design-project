package org.example.pojo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.pojo.base.Base;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "wrong_question", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class WrongQuestion extends Base {

    @TableField("user_id")
    private Long userId;

    @TableField("question_id")
    private Long questionId;

    @TableField("exam_id")
    private Long examId;

    @TableField("paper_name")
    private String paperName;

    private String question;

    private String category;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> choice;

    @TableField("correct_answer")
    private String correctAnswer;

    private String parse;

    private Integer score;

    private String type;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("wrong_count")
    private Integer wrongCount;

    @TableField("correct_streak")
    private Integer correctStreak;

    private String status;

    @TableField("last_user_answer")
    private String lastUserAnswer;

    @TableField("first_wrong_time")
    private Date firstWrongTime;

    @TableField("last_wrong_time")
    private Date lastWrongTime;

    @TableField("last_practice_time")
    private Date lastPracticeTime;
}
