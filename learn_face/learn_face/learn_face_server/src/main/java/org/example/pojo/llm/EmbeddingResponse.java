package org.example.pojo.llm;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResponse {

    private String object;

    private List<EmbeddingData> data;

    private String model;

    private Usage usage;

    private String id;

    @Data
    public static class EmbeddingData {

        private List<Double> embedding;

        private int index;

        private String object;
    }

    @Data
    public static class Usage {

        private int prompt_tokens;

        private int total_tokens;
    }
}
