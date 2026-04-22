package org.example.handler;

import cn.hutool.json.JSONUtil;
import org.example.pojo.llm.ChatCompletionResponse;
import org.example.pojo.llm.EmbeddingResponse;
import org.example.pojo.llm.VersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmHandler {

    public static final String SYSTEM_PROMPT = "system";

    public static final String USER_PROMPT = "user";

    public static final String ASSISTANT_PROMPT = "assistant";

    public static final String ROLE_PROMPT = "role";

    public static final String CONTENT_PROMPT = "content";

    @Value("${llm_sk}")
    private String sk;

    private static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private static final String EMBEDDING_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    public String chat(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("model", "qwen-plus");
        param.put("messages", messages);
        param.put("stream", false);
        param.put("enable_thinking", false);
        String requestBody = JSONUtil.toJsonStr(param);
        HttpURLConnection connection = getHttpURLConnection(requestBody, CHAT_URL);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                ChatCompletionResponse bean = JSONUtil.toBean(response.toString(), ChatCompletionResponse.class);
                return bean.getChoices().get(0).getMessage().getContent();
            }
        } else {
            throw new RuntimeException("Request failed with HTTP code: " + responseCode);
        }
    }

    public List<Double> embedding(String content) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("model", "text-embedding-v4");
        param.put("input", content);
        param.put("dimension", 1024);
        param.put("encoding_format", "float");
        String requestBody = JSONUtil.toJsonStr(param);

        HttpURLConnection connection = getHttpURLConnection(requestBody, EMBEDDING_URL);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                EmbeddingResponse bean = JSONUtil.toBean(response.toString(), EmbeddingResponse.class);
                return bean.getData().get(0).getEmbedding();
            }
        } else {
            throw new RuntimeException("Request failed with HTTP code: " + responseCode);
        }
    }

    public double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        if (normA == 0 || normB == 0) {
            throw new IllegalArgumentException("Vectors must not be zero vectors");
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public Map<String, Object> assembleSystem(String question) {
        Map<String, Object> answer = new HashMap<>();
        answer.put(ROLE_PROMPT, SYSTEM_PROMPT);
        answer.put(CONTENT_PROMPT, question);
        return answer;
    }

    public Map<String, Object> assembleUser(String question) {
        Map<String, Object> answer = new HashMap<>();
        answer.put(ROLE_PROMPT, USER_PROMPT);
        answer.put(CONTENT_PROMPT, question);
        return answer;
    }

    public Map<String, Object> assembleAssistant(String question) {
        Map<String, Object> answer = new HashMap<>();
        answer.put(ROLE_PROMPT, ASSISTANT_PROMPT);
        answer.put(CONTENT_PROMPT, question);
        return answer;
    }

    private HttpURLConnection getHttpURLConnection(String requestBody, String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", sk);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public VersionResponse version(String base64) throws Exception {

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen3-vl-plus");

        // input 对应的 Map
        Map<String, Object> inputMap = new HashMap<>();

        // messages 列表
        List<Map<String, Object>> messagesList = new ArrayList<>();

        // 第一个 message 对象
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");

        // content 列表
        List<Map<String, String>> contentList = new ArrayList<>();

        // 图片内容
        Map<String, String> imageContent = new HashMap<>();
        imageContent.put("image", base64);
        contentList.add(imageContent);

        // 文本内容
        Map<String, String> textContent = new HashMap<>();
        textContent.put("text", "你是一个书法字体识别专家，不要废话直接给我输出是什么字体，并简要分析，不涉及书法相关直接回复无法回答");
        contentList.add(textContent);

        // 组装
        messageMap.put("content", contentList);
        messagesList.add(messageMap);
        inputMap.put("messages", messagesList);
        requestMap.put("input", inputMap);

        String jsonStr = JSONUtil.toJsonStr(requestMap);

        HttpURLConnection connection = getHttpURLConnection("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation", jsonStr, sk);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                VersionResponse bean = JSONUtil.toBean(response.toString(), VersionResponse.class);
                return JSONUtil.toBean(response.toString(), VersionResponse.class);
            }
        } else {
            throw new RuntimeException("Request failed with HTTP code: " + responseCode);
        }
    }

    public HttpURLConnection getHttpURLConnection(String address, String requestBody, String sk) throws IOException {
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", sk);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }
}
