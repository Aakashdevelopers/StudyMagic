package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Question implements Serializable {
    public String id;
    
    @SerializedName(value = "question_text", alternate = {"questionText", "question"})
    public String questionText;
    
    public List<String> options;
    
    @SerializedName(value = "correct_option_index", alternate = {"correctOptionIndex", "correct_option", "correctAnswer", "correct_index"})
    public int correctOptionIndex;

    @SerializedName(value = "e", alternate = {"explanation", "exp"})
    public String explanation;

    @SerializedName(value = "explanation_image_url", alternate = {"exp_image_url", "explanation_image", "exp_image", "e_img"})
    public String explanationImageUrl;

    @SerializedName(value = "image_url", alternate = {"imageUrl", "image", "question_image"})
    public String imageUrl;
    
    private Integer selectedOptionIndex = null;
    private boolean isMarkedForReview = false;
    private boolean isVisited = false;

    public Question() {
    }

    public Question(String id, String questionText, List<String> options, int correctOptionIndex) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public Question(String id, String questionText, List<String> options, int correctOptionIndex, String explanation) {
        this(id, questionText, options, correctOptionIndex);
        this.explanation = explanation;
    }

    public Question(String id, String questionText, String imageUrl, List<String> options, int correctOptionIndex, String explanation) {
        this(id, questionText, options, correctOptionIndex, explanation);
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getImageUrl() { return imageUrl; }
    public String getExplanation() { return explanation; }
    public String getExplanationImageUrl() { return explanationImageUrl; }

    public List<String> getOptions() {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options;
    }

    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public boolean isMarkedForReview() { return isMarkedForReview; }
    public void setMarkedForReview(boolean markedForReview) { isMarkedForReview = markedForReview; }
    public boolean isVisited() { return isVisited; }
    public void setVisited(boolean visited) { isVisited = visited; }
}