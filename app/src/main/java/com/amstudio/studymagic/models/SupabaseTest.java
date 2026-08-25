package com.amstudio.studymagic.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class SupabaseTest implements Serializable {
    public int id;
    
    @SerializedName("category_id")
    public String categoryId;

    @SerializedName("topic_id")
    public String topicId;
    
    public String title;
    public String description;
    public int duration;
    
    @SerializedName("questions_json")
    public String questionsJson;

    public Test toTest() {
        Type listType = new TypeToken<List<Question>>(){}.getType();
        List<Question> questions = new Gson().fromJson(questionsJson, listType);
        return new Test(String.valueOf(id), title, description, duration, questions);
    }
}