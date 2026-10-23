package com.amstudio.examuplift.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class MockSubcategory implements Serializable {
    @SerializedName("id")
    private long id;

    @SerializedName(value = "category_id", alternate = {"categoryId", "category_id_fk"})
    private long categoryId;

    @SerializedName(value = "title", alternate = {"name", "subcategory_name", "sub_category_name"})
    private String title;

    @SerializedName(value = "description", alternate = {"desc", "sub_description"})
    private String description;

    @SerializedName(value = "icon_url", alternate = {"icon", "iconUrl", "image_url"})
    private String iconUrl;

    @SerializedName(value = "is_active", alternate = {"active", "isActive"})
    private boolean isActive = true;

    @SerializedName("created_at")
    private String createdAt;

    public MockSubcategory() {}

    public MockSubcategory(long id, long categoryId, String title, String description, String iconUrl) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
