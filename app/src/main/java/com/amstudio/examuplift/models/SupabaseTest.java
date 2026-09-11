package com.amstudio.examuplift.models;

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
    public String testType;

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
        public int duration;
    }

    public Test toTest() {
        List<Question> allQuestions = new ArrayList<>();
        Gson gson = new Gson();
        boolean parsedFromQuestionsJson = false;

        if (questionsJson != null) {
            try {
                String jsonStr = (questionsJson instanceof String) ? (String) questionsJson : gson.toJson(questionsJson);
                if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                    String trimmed = jsonStr.trim();
                    if (trimmed.startsWith("{")) {
                        Map<String, Object> map = gson.fromJson(trimmed, new TypeToken<Map<String, Object>>(){}.getType());
                        if (map != null && map.containsKey("subjects")) {
                            Type subjectListType = new TypeToken<List<SubjectModel>>(){}.getType();
                            List<SubjectModel> parsedSubjects = gson.fromJson(gson.toJson(map.get("subjects")), subjectListType);
                            if (parsedSubjects != null && !parsedSubjects.isEmpty()) {
                                subjects = parsedSubjects;
                            }
                            
                            if (map.containsKey("test_type") && map.get("test_type") != null) {
                                testType = String.valueOf(map.get("test_type"));
                            }
                            if (map.containsKey("is_subject_timer_enabled") && map.get("is_subject_timer_enabled") != null) {
                                try {
                                    isSubjectTimerEnabled = Boolean.parseBoolean(String.valueOf(map.get("is_subject_timer_enabled")));
                                } catch (Exception ignored) {}
                            }
                        } else if (map != null && map.containsKey("questions")) {
                            Type listType = new TypeToken<List<Question>>(){}.getType();
                            List<Question> qs = gson.fromJson(gson.toJson(map.get("questions")), listType);
                            if (qs != null && !qs.isEmpty()) {
                                allQuestions.addAll(qs);
                                parsedFromQuestionsJson = true;
                            }
                        } else if (map != null) {
                            for (Map.Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                if ("test_type".equalsIgnoreCase(key) || "is_subject_timer_enabled".equalsIgnoreCase(key)) continue;
                                Type listType = new TypeToken<List<Question>>(){}.getType();
                                List<Question> qs = gson.fromJson(gson.toJson(entry.getValue()), listType);
                                if (qs != null && !qs.isEmpty()) {
                                    allQuestions.addAll(qs);
                                    parsedFromQuestionsJson = true;
                                }
                            }
                        }
                    } else if (trimmed.startsWith("[")) {
                        Type listType = new TypeToken<List<Question>>(){}.getType();
                        List<Question> qs = gson.fromJson(trimmed, listType);
                        if (qs != null && !qs.isEmpty()) {
                            allQuestions.addAll(qs);
                            parsedFromQuestionsJson = true;
                        }
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

        if (!parsedFromQuestionsJson && subjects != null) {
            for (SubjectModel sm : subjects) {
                if (sm.questionsJson == null) continue;
                String jsonStr = gson.toJson(sm.questionsJson);
                Type listType = new TypeToken<List<Question>>(){}.getType();
                List<Question> qs = gson.fromJson(jsonStr, listType);
                if (qs != null) allQuestions.addAll(qs);
            }
        }
        return new Test(String.valueOf(id), title, description, duration, allQuestions);
    }
}