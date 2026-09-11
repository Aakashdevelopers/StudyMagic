package com.amstudio.examuplift.models.ai;

import java.util.List;

public class ChatResponse {
    public List<Choice> choices;
    public Boolean ok;
    public String message;
    public String error;
    public Details details;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String content;
    }

    public static class Details {
        public String reason;
    }
}