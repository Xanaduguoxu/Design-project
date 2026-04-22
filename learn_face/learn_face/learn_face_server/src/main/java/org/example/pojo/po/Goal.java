package org.example.pojo.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.pojo.base.Base;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Goal extends Base {

    private Long userId;

    private String title;

    private String description;

    private Date startDate;

    private Date endDate;

    private Integer targetValue;

    private Integer currentValue;

    private String status; //'状态: 进行中, 已完成, 放弃',

}
