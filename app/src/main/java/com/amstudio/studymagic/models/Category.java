package com.amstudio.studymagic.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Category implements Serializable {
    private String id;
    private String name;
    
    @SerializedName("icon_url")
    private String iconUrl;
    
    private int iconRes; // Keep it for local fallbacks if needed

    public Category(String id, String name, String iconUrl) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public Category(String id, String name, int iconRes) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIconUrl() { return iconUrl; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
}