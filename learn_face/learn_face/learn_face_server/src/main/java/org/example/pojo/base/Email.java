package org.example.pojo.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 功能描述: 邮件类
 *
 * @author zb
 * @date 2024/3/9
 * @return
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    /**
     * 主题
     */
    private String subject;

    /**
     * 邮件内容
     */
    private String content;

    /**
     * 收件人地址
     */
    private String receiver;

    /**
     * 发送者
     */
    private String sender;

    /**
     * 附件列表
     */
    private List<EmailAttachment> attachmentDtoList;

    private List<MultipartFile> multipartFiles;
}
