package org.example.pojo.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Dictionary extends Base {

    private String code;

    private String value;

    private String image;

    private String classify;

    private String status;
}


