package org.example.pojo.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;


@Data
@EqualsAndHashCode(callSuper = true)
public class Medal extends Base {

    private String name;

    private String brief;

    private String icon;

    private Integer requirements;
}
