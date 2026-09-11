package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Note implements Serializable {
    @SerializedName("id")
    public Long id;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("pdf_url")
    public String pdfUrl;

    @SerializedName("created_at")
    public String createdAt;

    public Note(String title, String description, String pdfUrl) {
        this.title = title;
        this.description = description;
        this.pdfUrl = pdfUrl;
    }
}