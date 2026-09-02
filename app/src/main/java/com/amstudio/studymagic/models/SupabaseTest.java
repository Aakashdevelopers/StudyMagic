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

    @SerializedName("test_type")
    public String testType; // "type1" / "universal" OR "type2" / "subject_wise"

    @SerializedName("is_subject_timer_enabled")
    public boolean isSubjectTimerEnabled = false;

    @SerializedName("subjects")
    public List<SubjectModel> subjects;

    @SerializedName("questions_json")
    public Object questionsJson;

    public static class SubjectModel implements Serializable {
        @SerializedName("id")
        public String id;

        @SerializedName("subject_name")
        public String subjectName;
        
        @SerializedName("questions_json")
        public Object questionsJson;
        
        @SerializedName("duration")
        public int duration; // In minutes
    }

    public Test toTest() {
        List<Question> allQuestions = new ArrayList<>();
        Gson gson = new Gson();

        // Check if subjects are inside questionsJson (Nested case like in your screenshot)
        if (subjects == null || subjects.isEmpty()) {
            try {
                String jsonStr = (questionsJson instanceof String) ? (String) questionsJson : gson.toJson(questionsJson);
                Map<String, Object> map = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (map != null && map.containsKey("subjects")) {
                    Type subjectListType = new TypeToken<List<SubjectModel>>(){}.getType();
                    subjects = gson.fromJson(gson.toJson(map.get("subjects")), subjectListType);
                    
                    if (map.containsKey("test_type")) {
                        testType = (String) map.get("test_type");
                    }
                    if (map.containsKey("is_subject_timer_enabled")) {
                        isSubjectTimerEnabled = (boolean) map.get("is_subject_timer_enabled");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("type2".equalsIgnoreCase(testType) || "subject_wise".equalsIgnoreCase(testType)) {
            isSubjectTimerEnabled = true;
        } else if ("type1".equalsIgnoreCase(testType) || "universal".equalsIgnoreCase(testType)) {
            isSubjectTimerEnabled = false;
        }

        if (isSubjectTimerEnabled && duration <= 0 && subjects != null) {
            int totalMins = 0;
            for (SubjectModel sm : subjects) {
                totalMins += sm.duration;
            }
            duration = totalMins;
        }

        if (subjects != null) {
            for (SubjectModel sm : subjects) {
                String jsonStr = gson.toJson(sm.questionsJson);
                Type listType = new TypeToken<List<Question>>(){}.getType();
                List<Question> qs = gson.fromJson(jsonStr, listType);
                if (qs != null) allQuestions.addAll(qs);
            }
        }
        return new Test(String.valueOf(id), title, description, duration, allQuestions);
    }
}
