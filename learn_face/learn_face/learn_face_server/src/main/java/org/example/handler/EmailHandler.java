package org.example.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.pojo.base.Email;
import org.example.pojo.base.EmailAttachment;
import org.example.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class EmailHandler {

    @Resource
    private JavaMailSenderImpl javaMailSender;

    public static EmailHandler emailHandler;

    @PostConstruct
    public void init() {
        emailHandler = this;
        emailHandler.javaMailSender = this.javaMailSender;
    }

    @Value("${spring.mail.properties.from}")
    private String sender;

    @Value("${spring.application.name}")
    private String subject;

    public boolean sendOfFile(String receiver, List<MultipartFile> files, String content) {
        try {
            if (!RegexUtils.checkEmail(receiver)) {
                throw new RuntimeException("收件人邮箱非法!");
            }

            if (CollectionUtils.isEmpty(files)) {
                throw new RuntimeException("文件为空!");
            }

            Email email = Email.builder()
                    .subject(subject)
                    .content(content)
                    .receiver(receiver)
                    .sender(sender)
                    .multipartFiles(files)
                    .build();
            emailHandler.sendMail(email);

            return true;
        } catch (Exception e) {
            log.info("sendEmail error:{}", e.getMessage());
            return false;
        }
    }

    public boolean sendOfContext(String receiver, String content) {
        try {
            boolean emailCheck = RegexUtils.checkEmail(receiver);
            if (!emailCheck) {
                throw new RuntimeException("收件人邮箱非法!");
            }

            Email email = Email.builder()
                    .subject(subject)
                    .content(content)
                    .receiver(receiver)
                    .sender(sender)
                    .build();
            emailHandler.sendMail(email);

            return true;
        } catch (Exception e) {
            log.info("sendEmail error:{}", e.getMessage());
            return false;
        }
    }

    private void sendMail(Email email) throws Exception {

        MimeMessage mimeMessage = emailHandler.javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setSubject(email.getSubject());
        helper.setText(email.getContent());
        helper.setTo(email.getReceiver());
        helper.setFrom(email.getSender());

        //  路径
        if (!CollectionUtils.isEmpty(email.getAttachmentDtoList())) {
            List<EmailAttachment> attachmentDtoList = email.getAttachmentDtoList();
            for (EmailAttachment attachmentDto : attachmentDtoList) {
                helper.addAttachment(attachmentDto.getFileName(), new File(attachmentDto.getFilePath()));
            }
        }

        //  文件
        if (!CollectionUtils.isEmpty(email.getMultipartFiles())) {
            List<MultipartFile> multipartFiles = email.getMultipartFiles();
            for (MultipartFile file : multipartFiles) {
                helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename()), file);
            }
        }

        emailHandler.javaMailSender.send(mimeMessage);
    }

}
