package org.example.pojo.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;

@Data
@EqualsAndHashCode(callSuper = true)
public class LearnLog extends Base {

    private Long learnId;

    private String name;

    private String chapters;

    private String lessons;

    private Long duration;
}
