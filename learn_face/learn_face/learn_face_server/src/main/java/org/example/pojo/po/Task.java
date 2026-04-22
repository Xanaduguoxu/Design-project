package org.example.pojo.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.pojo.base.Base;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Task extends Base {

    private String name;

    private String question;

    private String choice;

    private String answer;

    private String parse;

    private String category;

    private String type;

    private Integer score;

    private Integer totalScore;

    private String knowledgePoint;
}
