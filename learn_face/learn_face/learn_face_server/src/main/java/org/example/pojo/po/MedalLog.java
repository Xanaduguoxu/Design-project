package org.example.pojo.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;

import java.util.Date;


@Data
@EqualsAndHashCode(callSuper = true)
public class MedalLog extends Base {

    private Long medalId;

    private String name;

    private String brief;

    private String icon;

    private String unlocked;

    private Date obtainedDate;

}
