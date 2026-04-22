package org.example.controller.common;

import cn.hutool.json.JSONObject;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.handler.EmailHandler;
import org.example.handler.LlmHandler;
import org.example.handler.MinioHandler;
import org.example.handler.QrCodeHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private MinioHandler minioHandler;

    @Resource
    private EmailHandler emailHandler;

    @Resource
    private QrCodeHandler qrCodeHandler;

    @Resource
    private LlmHandler llmHandler;

    @PostMapping("/file/upload")
    public Result<String> upload(MultipartFile file) throws IOException {
        return Result.success(minioHandler.upload(file));
    }

    @GetMapping("/email/sendOfContext")
    public Result<Boolean> sendOfContext(@RequestParam("receiver") String receiver, @RequestParam("content") String content) {
        return Result.success(emailHandler.sendOfContext(receiver, content));
    }

    @PostMapping("/qrCode")
    public Result<String> qrcode(@RequestBody JSONObject req) {
        return Result.success(qrCodeHandler.qrCode(req));
    }

    @PostMapping("/recognize")
    public Result<String> recognize(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String base64 = "data:" + contentType + ";base64," +
                Base64.getEncoder().encodeToString(file.getBytes());
        return Result.success(llmHandler.version(base64).getOutput().getChoices().get(0).getMessage().getContent().get(0).getText());
    }
}
