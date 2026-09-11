package com.amstudio.examuplift.models;

public class SearchResult {
    public enum Type { TEST, NOTE, CATEGORY }

    private final Type type;
    private final String title;
    private final String subtitle;
    private final Object data;

    public SearchResult(Type type, String title, String subtitle, Object data) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.data = data;
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public Object getData() { return data; }
}