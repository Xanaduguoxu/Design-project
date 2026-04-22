package org.example.pojo.po.algorithm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;

import java.math.BigDecimal;

//  权重
@Data
@EqualsAndHashCode(callSuper = true)
public class Weight extends Base {

    private String name;

    private BigDecimal weight;
}
