package com.amstudio.examuplift.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.amstudio.examuplift.models.Question;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SavedQuestionsManager {
    private static final String PREF_NAME = "saved_questions_pref";
    private static final String KEY_SAVED_QUESTIONS = "saved_questions_list";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static List<Question> getSavedQuestions(Context context) {
        String json = getPrefs(context).getString(KEY_SAVED_QUESTIONS, null);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Question>>() {}.getType();
            List<Question> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static String getQuestionUniqueId(Question q) {
        if (q == null) return "";
        if (q.getId() != null && !q.getId().trim().isEmpty()) {
            return q.getId().trim();
        }
        if (q.getQuestionText() != null) {
            return String.valueOf(q.getQuestionText().trim().hashCode());
        }
        return "";
    }

    public static boolean isQuestionSaved(Context context, Question question) {
        if (question == null) return false;
        String qId = getQuestionUniqueId(question);
        if (qId.isEmpty()) return false;

        List<Question> savedList = getSavedQuestions(context);
        for (Question q : savedList) {
            if (qId.equals(getQuestionUniqueId(q))) {
                return true;
            }
        }
        return false;
    }

    public static boolean toggleSaveQuestion(Context context, Question question) {
        if (question == null) return false;
        List<Question> savedList = getSavedQuestions(context);
        String qId = getQuestionUniqueId(question);
        if (qId.isEmpty()) return false;

        boolean exists = false;
        Iterator<Question> iterator = savedList.iterator();
        while (iterator.hasNext()) {
            Question q = iterator.next();
            if (qId.equals(getQuestionUniqueId(q))) {
                iterator.remove();
                exists = true;
                break;
            }
        }

        if (!exists) {
            savedList.add(0, question);
        }

        saveList(context, savedList);
        return !exists;
    }

    public static void removeQuestion(Context context, Question question) {
        if (question == null) return;
        List<Question> savedList = getSavedQuestions(context);
        String qId = getQuestionUniqueId(question);
        if (qId.isEmpty()) return;

        Iterator<Question> iterator = savedList.iterator();
        while (iterator.hasNext()) {
            Question q = iterator.next();
            if (qId.equals(getQuestionUniqueId(q))) {
                iterator.remove();
                break;
            }
        }
        saveList(context, savedList);
    }

    public static void clearAllSavedQuestions(Context context) {
        getPrefs(context).edit().remove(KEY_SAVED_QUESTIONS).apply();
    }

    private static void saveList(Context context, List<Question> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        getPrefs(context).edit().putString(KEY_SAVED_QUESTIONS, json).apply();
    }
}
