package com.amstudio.examuplift.utils;

import android.os.Handler;
import android.os.Looper;

import com.amstudio.examuplift.models.Question;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuestionTranslator {

    public interface OnTranslationCompleteListener {
        void onSuccess(String translatedQuestionText, List<String> translatedOptions);
        void onError(Exception e);
    }

    public static class QuestionTranslation {
        public String questionText;
        public List<String> options;

        public QuestionTranslation(String questionText, List<String> options) {
            this.questionText = questionText;
            this.options = options;
        }
    }

    private static final Map<String, QuestionTranslation> cache = new HashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void translateQuestion(Question question, OnTranslationCompleteListener listener) {
        translateQuestion(question, "hi", listener);
    }

    public static void translateQuestion(Question question, String targetLang, OnTranslationCompleteListener listener) {
        if (question == null) return;
        final String lang = (targetLang == null || targetLang.trim().isEmpty()) ? "hi" : targetLang.trim().toLowerCase(Locale.ROOT);

        // 1. Check if question already has translation for targetLang
        if ("hi".equalsIgnoreCase(lang)) {
            if (question.getQuestionTextHindi() != null && !question.getQuestionTextHindi().trim().isEmpty()
                    && question.getOptionsHindi() != null && !question.getOptionsHindi().isEmpty()) {
                if (listener != null) listener.onSuccess(question.getQuestionTextHindi(), question.getOptionsHindi());
                return;
            }
        } else if ("en".equalsIgnoreCase(lang)) {
            if (question.getQuestionTextEnglish() != null && !question.getQuestionTextEnglish().trim().isEmpty()
                    && question.getOptionsEnglish() != null && !question.getOptionsEnglish().isEmpty()) {
                if (listener != null) listener.onSuccess(question.getQuestionTextEnglish(), question.getOptionsEnglish());
                return;
            }
        }

        // 2. Check in-memory translation cache
        String cacheKey = (question.getId() != null ? question.getId() : "") + "_" 
                + (question.getQuestionText() != null ? question.getQuestionText().hashCode() : 0) + "_" + lang;
        if (cache.containsKey(cacheKey) && cache.get(cacheKey) != null) {
            QuestionTranslation qt = cache.get(cacheKey);
            if (listener != null) listener.onSuccess(qt.questionText, qt.options);
            return;
        }

        // 3. Asynchronously translate via Google Translate API
        executor.execute(() -> {
            try {
                List<String> originalOpts = question.getOptions();
                StringBuilder sb = new StringBuilder();
                sb.append(question.getQuestionText() != null ? question.getQuestionText() : "");

                List<Integer> textOptIndices = new ArrayList<>();
                for (int i = 0; i < originalOpts.size(); i++) {
                    String opt = originalOpts.get(i);
                    sb.append("\n---\n");
                    if (opt != null && !isImageUrl(opt)) {
                        sb.append(opt);
                        textOptIndices.add(i);
                    } else {
                        sb.append("IMAGE_OPT");
                    }
                }

                String textToTranslate = sb.toString();
                String translatedRaw = fetchGoogleTranslation(textToTranslate, lang);

                if (translatedRaw != null && !translatedRaw.isEmpty()) {
                    String[] parts = translatedRaw.split("\n---\n|\\s*---\\s*");
                    String translatedQText = parts.length > 0 ? parts[0].trim() : question.getQuestionText();

                    List<String> translatedOpts = new ArrayList<>(originalOpts);
                    for (int i = 0; i < originalOpts.size(); i++) {
                        int partIdx = i + 1;
                        if (partIdx < parts.length && textOptIndices.contains(i)) {
                            translatedOpts.set(i, parts[partIdx].trim());
                        }
                    }

                    if ("hi".equalsIgnoreCase(lang)) {
                        question.setQuestionTextHindi(translatedQText);
                        question.setOptionsHindi(translatedOpts);
                    } else if ("en".equalsIgnoreCase(lang)) {
                        question.setQuestionTextEnglish(translatedQText);
                        question.setOptionsEnglish(translatedOpts);
                    }

                    QuestionTranslation qt = new QuestionTranslation(translatedQText, translatedOpts);
                    cache.put(cacheKey, qt);

                    if (listener != null) {
                        mainHandler.post(() -> listener.onSuccess(translatedQText, translatedOpts));
                    }
                } else {
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError(new Exception("Translation response empty")));
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onError(e));
                }
            }
        });
    }

    public static void pretranslateBatch(List<Question> questions, int startIndex, int batchSize, String targetLang) {
        if (questions == null || questions.isEmpty() || startIndex < 0 || startIndex >= questions.size()) {
            return;
        }
        int endIndex = Math.min(startIndex + batchSize, questions.size());
        for (int i = startIndex; i < endIndex; i++) {
            Question q = questions.get(i);
            if (q == null) continue;

            translateQuestion(q, targetLang, null);
        }
    }

    private static boolean isImageUrl(String text) {
        if (text == null) return false;
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
                || trimmed.contains("i.ibb.co/") || trimmed.contains(".png") 
                || trimmed.contains(".jpg") || trimmed.contains(".jpeg") || trimmed.contains(".webp");
    }

    private static String fetchGoogleTranslation(String text, String targetLang) throws Exception {
        String encodedText = URLEncoder.encode(text, "UTF-8");
        String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                + targetLang + "&dt=t&q=" + encodedText;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JsonArray jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();
            if (jsonArray.size() > 0 && jsonArray.get(0).isJsonArray()) {
                JsonArray sentences = jsonArray.get(0).getAsJsonArray();
                StringBuilder translatedResult = new StringBuilder();
                for (int i = 0; i < sentences.size(); i++) {
                    if (sentences.get(i).isJsonArray()) {
                        JsonArray sentence = sentences.get(i).getAsJsonArray();
                        if (sentence.size() > 0 && !sentence.get(0).isJsonNull()) {
                            translatedResult.append(sentence.get(0).getAsString());
                        }
                    }
                }
                return translatedResult.toString();
            }
        }
        return null;
    }
}
