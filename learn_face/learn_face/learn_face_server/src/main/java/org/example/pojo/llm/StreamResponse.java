package org.example.pojo.llm;

import lombok.Data;

import java.util.List;

@Data
public class StreamResponse {

    private Output output;

    private Usage usage;

    private String requestId;

    @Data
    public static class Output {

        private String sessionId;

        private String finishReason;

        private String text;

    }

    @Data
    public static class Usage {
        private List<Model> models;

    }

    @Data
    public static class Model {

        private int inputTokens;

        private int outputTokens;

        private String modelId;
    }
}

