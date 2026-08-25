package com.amstudio.studymagic.models;

import java.io.Serializable;
import java.util.List;

public class Test implements Serializable {
    private String id;
    private String title;
    private String description;
    private int durationMinutes;
    private List<Question> questions;

    public Test(String id, String title, String description, int durationMinutes, List<Question> questions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.questions = questions;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<Question> getQuestions() { return questions; }
}