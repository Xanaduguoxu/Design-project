package org.example.pojo.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachment {

    /**
     * 附件文件名
     */
    private String fileName;


    /**
     * 附件路径(绝对路径)
     */
    private String filePath;
}
