package com.amstudio.studymagic.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Note implements Serializable {
    @SerializedName("id")
    public Long id; // Use Long to match int8

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("pdf_url")
    public String pdfUrl;

    @SerializedName("created_at")
    public String createdAt;

    // Constructor for creating a new Note (id and createdAt will be null)
    public Note(String title, String description, String pdfUrl) {
        this.title = title;
        this.description = description;
        this.pdfUrl = pdfUrl;
    }
}