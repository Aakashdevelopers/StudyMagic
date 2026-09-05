package com.amstudio.studymagic.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    public String id;
    
    @SerializedName("question_text")
    public String questionText;
    
    public List<String> options;
    
    @SerializedName("correct_option_index")
    public int correctOptionIndex;

    @SerializedName(value = "e", alternate = {"explanation"})
    public String explanation;

    @SerializedName("image_url")
    public String imageUrl;
    
    private Integer selectedOptionIndex = null;
    private boolean isMarkedForReview = false;

    public Question(String id, String questionText, List<String> options, int correctOptionIndex) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getOptions() { return options; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public boolean isMarkedForReview() { return isMarkedForReview; }
    public void setMarkedForReview(boolean markedForReview) { isMarkedForReview = markedForReview; }
}