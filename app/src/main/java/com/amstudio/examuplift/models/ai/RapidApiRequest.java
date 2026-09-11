package com.amstudio.examuplift.models.ai;

import java.util.List;

public class RapidApiRequest {
    public List<Message> messages;
    public String system_prompt;
    public double temperature;
    public int top_k;
    public double top_p;
    public int max_tokens;
    public boolean web_access;

    public RapidApiRequest(List<Message> messages) {
        this.messages = messages;
        this.system_prompt = "";
        this.temperature = 0.7;
        this.top_k = 5;
        this.top_p = 0.9;
        this.max_tokens = 1000;
        this.web_access = false;
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}