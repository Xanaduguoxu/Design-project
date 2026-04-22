package org.example.pojo.llm;

import lombok.Data;

import java.util.List;

@Data
public class ChatCompletionChunk {

    private List<Choice> choices;

    @Data
    public static class Choice {

        private Delta delta;

        private String finishReason;

        private int index;

        private Object logprobs;
    }

    @Data
    public static class Delta {

        private String content;
    }
}
