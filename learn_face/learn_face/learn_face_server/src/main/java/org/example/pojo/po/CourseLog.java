package org.example.pojo.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourseLog extends Base {

    private Long courseId;

    private Long userId;

    private Long totalTime;
}
