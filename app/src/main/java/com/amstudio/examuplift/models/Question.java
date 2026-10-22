package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Question implements Serializable {
    public String id;
    
    @SerializedName(value = "question_text", alternate = {"questionText", "question"})
    public String questionText;

    @SerializedName(value = "question_text_hindi", alternate = {"questionTextHindi", "question_hindi", "question_hi", "question_text_hi"})
    public String questionTextHindi;

    @SerializedName(value = "question_text_english", alternate = {"questionTextEnglish", "question_english", "question_en", "question_text_en"})
    public String questionTextEnglish;
    
    public List<String> options;

    @SerializedName(value = "options_hindi", alternate = {"optionsHindi", "options_hi"})
    public List<String> optionsHindi;

    @SerializedName(value = "options_english", alternate = {"optionsEnglish", "options_en"})
    public List<String> optionsEnglish;
    
    @SerializedName(value = "correct_option_index", alternate = {"correctOptionIndex", "correct_option", "correctAnswer", "correct_index"})
    public int correctOptionIndex;

    @SerializedName(value = "e", alternate = {"explanation", "exp"})
    public String explanation;

    @SerializedName(value = "explanation_hindi", alternate = {"explanationHindi", "exp_hindi", "explanation_hi", "e_hi"})
    public String explanationHindi;

    @SerializedName(value = "explanation_english", alternate = {"explanationEnglish", "exp_english", "explanation_en", "e_en"})
    public String explanationEnglish;

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
    public String getQuestionTextHindi() { return questionTextHindi; }
    public void setQuestionTextHindi(String questionTextHindi) { this.questionTextHindi = questionTextHindi; }
    public String getQuestionTextEnglish() { return questionTextEnglish; }
    public void setQuestionTextEnglish(String questionTextEnglish) { this.questionTextEnglish = questionTextEnglish; }

    public String getImageUrl() { return imageUrl; }
    public String getExplanation() { return explanation; }
    public String getExplanationHindi() { return explanationHindi; }
    public void setExplanationHindi(String explanationHindi) { this.explanationHindi = explanationHindi; }
    public String getExplanationEnglish() { return explanationEnglish; }
    public void setExplanationEnglish(String explanationEnglish) { this.explanationEnglish = explanationEnglish; }
    public String getExplanationImageUrl() { return explanationImageUrl; }

    public List<String> getOptions() {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options;
    }

    public List<String> getOptionsHindi() {
        if (optionsHindi == null) {
            optionsHindi = new ArrayList<>();
        }
        return optionsHindi;
    }

    public void setOptionsHindi(List<String> optionsHindi) {
        this.optionsHindi = optionsHindi;
    }

    public List<String> getOptionsEnglish() {
        if (optionsEnglish == null) {
            optionsEnglish = new ArrayList<>();
        }
        return optionsEnglish;
    }

    public void setOptionsEnglish(List<String> optionsEnglish) {
        this.optionsEnglish = optionsEnglish;
    }

    public String getQuestionTextForLanguage(String lang) {
        if ("hi".equalsIgnoreCase(lang) || "hindi".equalsIgnoreCase(lang)) {
            if (questionTextHindi != null && !questionTextHindi.trim().isEmpty()) {
                return questionTextHindi;
            }
        } else if ("en".equalsIgnoreCase(lang) || "english".equalsIgnoreCase(lang)) {
            if (questionTextEnglish != null && !questionTextEnglish.trim().isEmpty()) {
                return questionTextEnglish;
            }
        }
        return questionText != null ? questionText : "";
    }

    public List<String> getOptionsForLanguage(String lang) {
        if ("hi".equalsIgnoreCase(lang) || "hindi".equalsIgnoreCase(lang)) {
            if (optionsHindi != null && !optionsHindi.isEmpty()) {
                return optionsHindi;
            }
        } else if ("en".equalsIgnoreCase(lang) || "english".equalsIgnoreCase(lang)) {
            if (optionsEnglish != null && !optionsEnglish.isEmpty()) {
                return optionsEnglish;
            }
        }
        return getOptions();
    }

    public String getExplanationForLanguage(String lang) {
        if ("hi".equalsIgnoreCase(lang) || "hindi".equalsIgnoreCase(lang)) {
            if (explanationHindi != null && !explanationHindi.trim().isEmpty()) {
                return explanationHindi;
            }
        } else if ("en".equalsIgnoreCase(lang) || "english".equalsIgnoreCase(lang)) {
            if (explanationEnglish != null && !explanationEnglish.trim().isEmpty()) {
                return explanationEnglish;
            }
        }
        return explanation != null ? explanation : "";
    }

    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public boolean isMarkedForReview() { return isMarkedForReview; }
    public void setMarkedForReview(boolean markedForReview) { isMarkedForReview = markedForReview; }
    public boolean isVisited() { return isVisited; }
    public void setVisited(boolean visited) { isVisited = visited; }
}