package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AppFeedback implements Serializable {

    @SerializedName("id")
    public Long id;

    @SerializedName("rating")
    public Integer rating;

    @SerializedName("review")
    public String review;

    @SerializedName("feature_suggestion")
    public String featureSuggestion;

    @SerializedName("user_contact")
    public String userContact;

    @SerializedName("created_at")
    public String createdAt;

    public AppFeedback() {
    }

    public AppFeedback(Integer rating, String review, String featureSuggestion, String userContact) {
        this.rating = rating;
        this.review = review;
        this.featureSuggestion = featureSuggestion;
        this.userContact = userContact;
    }

    public Long getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public String getReview() {
        return review;
    }

    public String getFeatureSuggestion() {
        return featureSuggestion;
    }

    public String getUserContact() {
        return userContact;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
