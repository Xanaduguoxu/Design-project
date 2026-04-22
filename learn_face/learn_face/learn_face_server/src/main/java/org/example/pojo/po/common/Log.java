package org.example.pojo.po.common;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.base.Base;

import java.io.Serializable;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class Log extends Base implements Serializable {

    private static final long serialVersionUID = 1124124L;

    private String username;

    private String args;

    private String operation;

    private String url;

    private String ip;
}
