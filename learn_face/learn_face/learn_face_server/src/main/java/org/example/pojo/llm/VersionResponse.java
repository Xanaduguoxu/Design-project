package org.example.pojo.llm;

import lombok.Data;

import java.util.List;

@Data
public class VersionResponse {

    public Output output;
    public Usage usage;
    public String requestId;


    @Data
    public static class Output {
        public List<Choice> choices;
    }

    @Data
    public static class Choice {
        public String finishReason;
        public Message message;
    }

    @Data
    public static class Message {
        public List<Content> content;
        public String reasoningContent;
        public String role;
    }

    @Data
    public static class Content {
        public String text;
    }

    @Data
    public static class Usage {
        public int cachedTokens;
        public int imageTokens;
        public int inputTokens;
        public InputTokensDetails inputTokensDetails;
        public int outputTokens;
        public OutputTokensDetails outputTokensDetails;
        public int totalTokens;
    }

    @Data
    public static class InputTokensDetails {
        public int imageTokens;
        public int textTokens;
    }

    @Data
    public static class OutputTokensDetails {
        public int textTokens;
    }

}
