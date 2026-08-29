package com.amstudio.studymagic.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public Object questionsJson;

    public Test toTest() {
        Gson gson = new Gson();
        List<Question> allQuestions = new ArrayList<>();
        
        // Handle both String (text column) and Map/List (jsonb column)
        String jsonStr;
        if (questionsJson instanceof String) {
            jsonStr = (String) questionsJson;
        } else {
            jsonStr = gson.toJson(questionsJson);
        }
        
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr.equals("null")) {
            return new Test(String.valueOf(id), title, description, duration, allQuestions);
        }

        try {
            // Try parsing as Map<String, List<Question>> (New Format: Subject-wise)
            Type mapType = new TypeToken<Map<String, List<Question>>>(){}.getType();
            Map<String, List<Question>> subjectMap = gson.fromJson(jsonStr, mapType);
            
            // Check if it's actually a map of lists
            if (subjectMap != null && !subjectMap.isEmpty() && subjectMap.values().iterator().next() instanceof List) {
                for (Object value : subjectMap.values()) {
                    if (value instanceof List) {
                        allQuestions.addAll((List<Question>) value);
                    }
                }
            } else {
                // Try parsing as List<Question> (Old Format: Flat List)
                Type listType = new TypeToken<List<Question>>(){}.getType();
                List<Question> flatList = gson.fromJson(jsonStr, listType);
                if (flatList != null) {
                    allQuestions.addAll(flatList);
                }
            }
        } catch (Exception e) {
            // Fallback for any parsing errors
            try {
                Type listType = new TypeToken<List<Question>>(){}.getType();
                List<Question> flatList = gson.fromJson(jsonStr, listType);
                if (flatList != null) {
                    allQuestions.addAll(flatList);
                }
            } catch (Exception e2) {
                // Simple error handling
            }
        }

        return new Test(String.valueOf(id), title, description, duration, allQuestions);
    }
}