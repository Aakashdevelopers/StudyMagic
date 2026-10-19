package com.amstudio.examuplift.models;

import java.io.Serializable;
import java.util.List;

public class Test implements Serializable {
    private String id;
    private String title;
    private String description;
    private int durationMinutes;
    private List<Question> questions;
    private Double negativeMarking;
    private Double correctMarks;
    private Double totalMarks;

    public Test(String id, String title, String description, int durationMinutes, List<Question> questions) {
        this(id, title, description, durationMinutes, questions, null, 1.0, null);
    }

    public Test(String id, String title, String description, int durationMinutes, List<Question> questions, Double negativeMarking) {
        this(id, title, description, durationMinutes, questions, negativeMarking, 1.0, null);
    }

    public Test(String id, String title, String description, int durationMinutes, List<Question> questions, Double negativeMarking, Double correctMarks, Double totalMarks) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.questions = questions;
        this.negativeMarking = negativeMarking;
        this.correctMarks = correctMarks;
        this.totalMarks = totalMarks;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<Question> getQuestions() { return questions; }
    public Double getNegativeMarking() { return negativeMarking != null ? negativeMarking : 0.0; }
    public void setNegativeMarking(Double negativeMarking) { this.negativeMarking = negativeMarking; }
    public boolean hasNegativeMarking() { return negativeMarking != null && negativeMarking > 0; }

    public Double getCorrectMarks() { return (correctMarks != null && correctMarks > 0) ? correctMarks : 1.0; }
    public void setCorrectMarks(Double correctMarks) { this.correctMarks = correctMarks; }

    public Double getTotalMarks() {
        if (totalMarks != null && totalMarks > 0) return totalMarks;
        int qCount = (questions != null) ? questions.size() : 0;
        return qCount * getCorrectMarks();
    }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }
    public Double getMaxMarks() { return getTotalMarks(); }
}