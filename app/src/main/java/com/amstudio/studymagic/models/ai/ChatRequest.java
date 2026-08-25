package com.amstudio.studymagic.models.ai;

import java.util.List;

public class ChatRequest {
    public String model;
    public List<Message> messages;
    public int max_tokens;
    public double temperature;

    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
        this.max_tokens = 500; // Default limit for speed
        this.temperature = 0.7;
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