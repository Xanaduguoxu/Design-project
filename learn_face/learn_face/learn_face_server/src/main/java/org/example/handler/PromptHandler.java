package org.example.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptHandler {

    @Value("classpath:select-template.txt")
    private Resource selectResource;

    @Value("classpath:classify-template.txt")
    private Resource classifyResource;

    @Value("classpath:rag-template.txt")
    private Resource ragResource;

    @Value("classpath:question-template.txt")
    private Resource questionResource;

    public String loadSelectPrompt() throws IOException {
        return new String(FileCopyUtils.copyToByteArray(selectResource.getInputStream()), StandardCharsets.UTF_8);
    }

    public String loadClassifyPrompt() throws IOException {
        return new String(FileCopyUtils.copyToByteArray(classifyResource.getInputStream()), StandardCharsets.UTF_8);
    }

    public String loadRagPrompt() throws IOException {
        return new String(FileCopyUtils.copyToByteArray(ragResource.getInputStream()), StandardCharsets.UTF_8);
    }

    public String loadQuestionPrompt() throws IOException {
        return new String(FileCopyUtils.copyToByteArray(questionResource.getInputStream()), StandardCharsets.UTF_8);
    }
}
