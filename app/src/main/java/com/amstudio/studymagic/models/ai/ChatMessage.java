package com.amstudio.studymagic.models.ai;

public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    private String role;
    private String content;
    private int type;

    public ChatMessage(String role, String content, int type) {
        this.role = role;
        this.content = content;
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }
}