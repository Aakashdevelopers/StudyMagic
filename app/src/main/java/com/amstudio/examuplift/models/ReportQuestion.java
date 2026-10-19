package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ReportQuestion implements Serializable {
    @SerializedName("id")
    public Long id;

    @SerializedName("question_id")
    public String questionId;

    @SerializedName("question_text")
    public String questionText;

    @SerializedName("options")
    public List<String> options;

    @SerializedName("correct_option")
    public String correctOption;

    @SerializedName("reason")
    public String reason;

    @SerializedName("test_title")
    public String testTitle;

    @SerializedName("created_at")
    public String createdAt;

    public ReportQuestion() {
    }

    public ReportQuestion(String questionId, String questionText, List<String> options, String correctOption, String reason, String testTitle) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.options = options;
        this.correctOption = correctOption;
        this.reason = reason;
        this.testTitle = testTitle;
    }

    public Long getId() {
        return id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public String getReason() {
        return reason;
    }

    public String getTestTitle() {
        return testTitle;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
