package com.amstudio.examuplift.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amstudio.examuplift.AiChatActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.api.PollinationsApi;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.models.ai.ChatRequest;
import com.amstudio.examuplift.models.ai.ChatResponse;
import com.amstudio.examuplift.models.ai.RapidApiRequest;
import com.amstudio.examuplift.models.ai.RapidApiResponse;
import com.amstudio.examuplift.utils.Constants;
import com.amstudio.examuplift.utils.DialogUtils;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.noties.markwon.Markwon;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiAnalysisFragment extends Fragment {

    private static final String ARG_TEST = "arg_test";
    private static final String ARG_CORRECT = "arg_correct";
    private static final String ARG_TOTAL = "arg_total";

    private TextView tvAIAnalysis;
    private View aiLoading;
    private TextView tvAILoadingStatus;
    private TextView tvAIPillStatus;
    private TextView tvAIPillAccuracy;
    private Markwon markwon;

    private String selectedLanguage = "English";
    private Test currentTest;
    private int correctCount = 0;
    private int totalCount = 0;

    public static AiAnalysisFragment newInstance(Test test, int correct, int total) {
        AiAnalysisFragment fragment = new AiAnalysisFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TEST, test);
        args.putInt(ARG_CORRECT, correct);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTest = (Test) getArguments().getSerializable(ARG_TEST);
            correctCount = getArguments().getInt(ARG_CORRECT, 0);
            totalCount = getArguments().getInt(ARG_TOTAL, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_analysis, container, false);

        markwon = Markwon.create(requireContext());

        tvAIAnalysis = view.findViewById(R.id.tvAIAnalysisText);
        aiLoading = view.findViewById(R.id.aiAnalysisLoading);
        tvAILoadingStatus = view.findViewById(R.id.tvAILoadingStatus);
        tvAIPillStatus = view.findViewById(R.id.tvAIPillStatus);
        tvAIPillAccuracy = view.findViewById(R.id.tvAIPillAccuracy);
        View btnRefreshAI = view.findViewById(R.id.btnRefreshAI);
        View btnChatAI = view.findViewById(R.id.btnChatAI);

        ChipGroup chipGroupLanguage = view.findViewById(R.id.chipGroupLanguage);
        if (chipGroupLanguage != null) {
            chipGroupLanguage.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipHindi)) {
                    selectedLanguage = "Hindi";
                } else {
                    selectedLanguage = "English";
                }
                fetchAIPerformanceAnalysis(currentTest, correctCount, totalCount, selectedLanguage);
            });
        }

        if (btnRefreshAI != null) {
            btnRefreshAI.setOnClickListener(v -> fetchAIPerformanceAnalysis(currentTest, correctCount, totalCount, selectedLanguage));
        }

        if (btnChatAI != null) {
            btnChatAI.setOnClickListener(v -> {
                if (currentTest != null && getActivity() != null) {
                    Intent intent = new Intent(getActivity(), AiChatActivity.class);
                    intent.putExtra(AiChatActivity.EXTRA_TEST, currentTest);
                    intent.putExtra(AiChatActivity.EXTRA_CORRECT, correctCount);
                    intent.putExtra(AiChatActivity.EXTRA_TOTAL, totalCount);
                    startActivity(intent);
                }
            });
        }

        // Trigger AI Analysis
        fetchAIPerformanceAnalysis(currentTest, correctCount, totalCount, selectedLanguage);

        return view;
    }

    private void fetchAIPerformanceAnalysis(Test test, int correct, int total, String language) {
        int accuracy = total > 0 ? (correct * 100) / total : 0;
        boolean isHindi = "Hindi".equalsIgnoreCase(language);

        // Update Quick Metrics Bar
        if (tvAIPillAccuracy != null) {
            tvAIPillAccuracy.setText(String.format(Locale.getDefault(), "📊 %d%% Accuracy", accuracy));
        }
        if (tvAIPillStatus != null) {
            if (accuracy >= 80) {
                tvAIPillStatus.setText("🎯 High Proficiency");
            } else if (accuracy >= 50) {
                tvAIPillStatus.setText("⚡ Good Progress");
            } else {
                tvAIPillStatus.setText("⚠️ Focus Needed");
            }
        }

        if (aiLoading != null) aiLoading.setVisibility(View.VISIBLE);
        if (tvAILoadingStatus != null) {
            tvAILoadingStatus.setText(isHindi ? "AI प्रत्येक प्रश्न का गहन विश्लेषण व शॉर्टकट ट्रिक्स तैयार कर रहा है..." : "AI is analyzing each question deeply with shortcuts & tricks...");
        }

        // Build complete, detailed question data for EVERY single question for deep AI analysis
        StringBuilder performanceData = new StringBuilder();
        String testTitle = (test != null && test.getTitle() != null) ? test.getTitle() : "Practice Quiz";
        performanceData.append("Test Title: ").append(testTitle).append("\n");
        performanceData.append("Score: ").append(correct).append("/").append(total).append(" (").append(accuracy).append("% Accuracy)\n\n");
        performanceData.append("--- COMPLETE ITEM-BY-ITEM QUESTION LIST ---\n\n");

        if (test != null && test.getQuestions() != null && !test.getQuestions().isEmpty()) {
            int qSize = test.getQuestions().size();
            for (int i = 0; i < qSize; i++) {
                Question q = test.getQuestions().get(i);
                if (q == null) continue;

                Integer selectedIdx = q.getSelectedOptionIndex();
                boolean isCorrect = selectedIdx != null && Objects.equals(selectedIdx, q.getCorrectOptionIndex());
                boolean isSkipped = selectedIdx == null;

                String status = isCorrect ? "CORRECT ✓" : (isSkipped ? "SKIPPED ⚪" : "INCORRECT ✗");

                String qText = q.getQuestionTextForLanguage(language);
                if (qText == null || qText.trim().isEmpty()) {
                    qText = q.getQuestionText() != null ? q.getQuestionText() : "";
                }
                qText = qText.replaceAll("<[^>]*>", "").trim();

                List<String> options = q.getOptionsForLanguage(language);
                if (options == null || options.isEmpty()) {
                    options = q.getOptions();
                }

                String studentAnswer = "Not Attempted (Skipped)";
                if (selectedIdx != null && options != null && selectedIdx >= 0 && selectedIdx < options.size()) {
                    studentAnswer = "Option " + (char) ('A' + selectedIdx) + ": " + options.get(selectedIdx);
                }

                String correctAnswer = "N/A";
                int correctIdx = q.getCorrectOptionIndex();
                if (options != null && correctIdx >= 0 && correctIdx < options.size()) {
                    correctAnswer = "Option " + (char) ('A' + correctIdx) + ": " + options.get(correctIdx);
                } else if (options != null && correctIdx - 1 >= 0 && correctIdx - 1 < options.size()) {
                    correctAnswer = "Option " + (char) ('A' + (correctIdx - 1)) + ": " + options.get(correctIdx - 1);
                }

                String exp = q.getExplanationForLanguage(language);
                if (exp == null || exp.trim().isEmpty()) {
                    exp = q.getExplanation() != null ? q.getExplanation() : "";
                }
                exp = exp.replaceAll("<[^>]*>", "").trim();

                performanceData.append("Question ").append(i + 1).append(" [").append(status).append("]:\n");
                performanceData.append("Question Text: ").append(qText).append("\n");
                if (options != null && !options.isEmpty()) {
                    performanceData.append("Options: ");
                    for (int optI = 0; optI < options.size(); optI++) {
                        performanceData.append("(").append((char) ('A' + optI)).append(") ").append(options.get(optI)).append(" ");
                    }
                    performanceData.append("\n");
                }
                performanceData.append("Student Choice: ").append(studentAnswer).append("\n");
                performanceData.append("Correct Choice: ").append(correctAnswer).append("\n");
                if (!exp.isEmpty()) {
                    performanceData.append("Base Explanation: ").append(exp).append("\n");
                }
                performanceData.append("\n");
            }
        }

        String prompt;
        if (isHindi) {
            prompt = "आप Exam Uplift के छात्रों के लिए एक उत्कृष्ट शिक्षा विशेषज्ञ एवं AI Smart Analyst हैं।\n" +
                    "नीचे दिए गए परीक्षा परिणाम और **प्रत्येक प्रश्न (Each & Every Question)** का अत्यंत गहरा और विस्तृत विश्लेषण करें:\n\n" +
                    performanceData + "\n\n" +
                    "🎯 **मुख्य निर्देश (STRICT INSTRUCTIONS)**:\n" +
                    "1. आपको ऊपर दिए गए **प्रत्येक प्रश्न (Q1, Q2, Q3...) का एक-एक करके (Question-by-Question)** गहरा विश्लेषण प्रदान करना है।\n" +
                    "2. **सही उत्तरित प्रश्न (Correct)**, **गलत उत्तरित प्रश्न (Incorrect)** और **छूटे हुए प्रश्न (Skipped)** - तीनों श्रेणियों को सरल और स्पष्ट शब्दों में समझाएं।\n" +
                    "3. **प्रत्येक प्रश्न के लिए एक 'स्मार्ट ट्रिक / याद रखने की ट्रिक / शॉर्टकट' (Smart Trick / Memory Rule / Shortcut)** अवश्य दें ताकि छात्र परीक्षा में उसे आसानी से याद रख सके या तेजी से हल कर सके।\n\n" +
                    "STRICTLY निम्नलिखित Markdown प्रारूप में उत्तर प्रस्तुत करें:\n\n" +
                    "### 📊 Overall Performance & Deep Summary\n" +
                    "• **स्कोर**: " + correct + "/" + total + " (सटीकता: " + accuracy + "%) - " + testTitle + "\n" +
                    "• छात्र के समग्र प्रदर्शन का विस्तृत व प्रेरणादायक मूल्यांकन।\n\n" +
                    "### 📝 Question-by-Question Deep Analysis & Tricks (प्रत्येक प्रश्न का विस्तृत विश्लेषण व ट्रिक्स)\n\n" +
                    "नीचे टेस्ट के सभी प्रश्नों (Q1 से Q" + total + ") का क्रमवार गहन विश्लेषण है:\n\n" +
                    "#### Q1: [प्रश्न की मुख्य पंक्ति] - [🟢 CORRECT / 🔴 INCORRECT / ⚪ SKIPPED]\n" +
                    "• **आपका उत्तर**: [छात्र द्वारा चुना गया विकल्प या Skipped]\n" +
                    "• **सही उत्तर**: [सही विकल्प]\n" +
                    "• **गहन व्याख्या व अवधारणा (Deep Concept)**: सही उत्तर क्यों सही है और गलत उत्तर क्यों गलत था/क्या भ्रम हुआ था, इसे सरल शब्दों में गहराई से समझाएं।\n" +
                    "• **💡 स्मार्ट ट्रिक / याद रखने का तरीका (Smart Trick & Shortcut)**: इस प्रश्न या अवधारणा को याद रखने/हल करने की एक आसान ट्रिक, निमोनिक, शॉर्टकट या नियम दें।\n\n" +
                    "(इसी प्रारूप में सभी प्रश्नों Q1, Q2, Q3... Q" + total + " का विश्लेषण करें)\n\n" +
                    "### 🔴 Weak Topics & Revision Strategy\n" +
                    "• गलत और छूटे प्रश्नों से पहचाने गए कमजोर विषयों की सूची और उनके पुनरीक्षण के लिए स्पष्ट कदम।\n\n" +
                    "### ⚡ Exam Shortcuts & Time Saving Tips\n" +
                    "1. अगले प्रयास में समय बचाने और सिली मिस्टेक्स रोकने के लिए व्यावहारिक सुझाव।\n\n" +
                    "नोट: उत्तर हिंदी भाषा में दें (अनुभाग शीर्षक अंग्रेजी में रखें)।";
        } else {
            prompt = "You are an expert Educational Mentor & AI Smart Analyst for Exam Uplift.\n" +
                    "Analyze the following test result and conduct a DEEP QUESTION-BY-QUESTION ANALYSIS for all questions:\n\n" +
                    performanceData + "\n\n" +
                    "🎯 **CRITICAL INSTRUCTIONS**:\n" +
                    "1. You MUST provide a **Question-by-Question breakdown for EVERY SINGLE QUESTION (Q1, Q2, Q3...)** in the test.\n" +
                    "2. Thoroughly explain **Correct, Incorrect, AND Skipped questions** - why the correct option is right and why misconceptions occurred for wrong answers.\n" +
                    "3. For **EVERY question**, provide a **SMART TRICK / MEMORY MNEMONIC / SOLVING SHORTCUT (💡 Smart Trick)** so the student can easily memorize or solve similar questions quickly in future exams.\n\n" +
                    "Format the output strictly in Markdown using this structure:\n\n" +
                    "### 📊 Overall Performance & Deep Summary\n" +
                    "• **Score**: " + correct + "/" + total + " (" + accuracy + "% Accuracy) in " + testTitle + ".\n" +
                    "• Comprehensive performance evaluation and encouraging mentor insights.\n\n" +
                    "### 📝 Question-by-Question Deep Analysis & Tricks\n\n" +
                    "Below is the itemized analysis for all questions (Q1 to Q" + total + "):\n\n" +
                    "#### Q1: [Short Question Title] - [🟢 CORRECT / 🔴 INCORRECT / ⚪ SKIPPED]\n" +
                    "• **Your Answer**: [Option selected or Skipped]\n" +
                    "• **Correct Answer**: [Correct Option]\n" +
                    "• **Deep Concept & Explanation**: Explain why the correct option is right and why wrong/skipped options happened.\n" +
                    "• **💡 Smart Trick & Memory Mnemonic**: A practical shortcut, formula rule, memory trick, or mnemonic to solve/remember this question quickly.\n\n" +
                    "(Repeat for ALL questions Q1, Q2, Q3... Q" + total + ")\n\n" +
                    "### 🔴 Weak Topics & Revision Strategy\n" +
                    "• Specific weak concepts derived from wrong/skipped questions and how to revise them effectively.\n\n" +
                    "### ⚡ Exam Shortcuts & Time Saving Tips\n" +
                    "1. Proven exam strategies to boost accuracy and save time in future tests.";
        }

        // TIER 1: OpenRouter
        if (Constants.OPENROUTER_KEY != null && !Constants.OPENROUTER_KEY.trim().isEmpty()) {
            tryOpenRouterAI(prompt, Constants.OPENROUTER_MODEL, test, correct, total, language);
        } else {
            // Tier 2: Pollinations AI
            tryPollinationsAI(prompt, test, correct, total, language);
        }
    }

    private void tryOpenRouterAI(String prompt, String modelName, Test test, int correct, int total, String language) {
        if (tvAILoadingStatus != null) {
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "OpenRouter AI से एक-एक प्रश्न का विश्लेषण हो रहा है..." : "Analyzing each question with OpenRouter AI...");
        }

        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatRequest.Message("user", prompt));
        ChatRequest request = new ChatRequest(modelName, messages);

        String authHeader = "Bearer " + Constants.OPENROUTER_KEY.trim();
        ApiClient.getOpenRouterInterface().getCompletion(
                authHeader,
                "https://examuplift.app",
                "Exam Uplift App",
                request
        ).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                    ChatResponse.Choice choice = response.body().choices.get(0);
                    if (choice != null && choice.message != null && !TextUtils.isEmpty(choice.message.content)) {
                        if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                        markwon.setMarkdown(tvAIAnalysis, choice.message.content);
                        return;
                    }
                }

                // If primary OpenRouter model failed/busy, try fallback model or Pollinations
                if (Objects.equals(modelName, Constants.OPENROUTER_MODEL) && Constants.OPENROUTER_FALLBACK_MODEL != null) {
                    tryOpenRouterAI(prompt, Constants.OPENROUTER_FALLBACK_MODEL, test, correct, total, language);
                } else {
                    tryPollinationsAI(prompt, test, correct, total, language);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (Objects.equals(modelName, Constants.OPENROUTER_MODEL) && Constants.OPENROUTER_FALLBACK_MODEL != null) {
                    tryOpenRouterAI(prompt, Constants.OPENROUTER_FALLBACK_MODEL, test, correct, total, language);
                } else {
                    tryPollinationsAI(prompt, test, correct, total, language);
                }
            }
        });
    }

    private void tryPollinationsAI(String prompt, Test test, int correct, int total, String language) {
        if (tvAILoadingStatus != null) {
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "AI इंजन से एक-एक प्रश्न का विश्लेषण हो रहा है..." : "AI engine analyzing questions...");
        }

        PollinationsApi.PollinationsRequest req = new PollinationsApi.PollinationsRequest(prompt);
        ApiClient.getPollinationsInterface().getCompletion(req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        String result = body.string();
                        if (!TextUtils.isEmpty(result)) {
                            if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                            markwon.setMarkdown(tvAIAnalysis, result);
                            return;
                        }
                    } catch (IOException ignored) {}
                }
                tryRapidAPI(prompt, test, correct, total, language);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                tryRapidAPI(prompt, test, correct, total, language);
            }
        });
    }

    private void tryRapidAPI(String prompt, Test test, int correct, int total, String language) {
        if (Constants.RAPIDAPI_KEY != null && !Constants.RAPIDAPI_KEY.isEmpty()) {
            List<RapidApiRequest.Message> messages = new ArrayList<>();
            messages.add(new RapidApiRequest.Message("user", prompt));
            RapidApiRequest request = new RapidApiRequest(messages);

            ApiClient.getAIInterface().getCompletion(Constants.RAPIDAPI_HOST, Constants.RAPIDAPI_KEY, request)
                    .enqueue(new Callback<RapidApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<RapidApiResponse> call, @NonNull Response<RapidApiResponse> response) {
                            if (!isAdded()) return;
                            if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                            if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().result)) {
                                markwon.setMarkdown(tvAIAnalysis, response.body().result);
                            } else {
                                renderLocalAIPerformanceReport(test, correct, total, language);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<RapidApiResponse> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            renderLocalAIPerformanceReport(test, correct, total, language);
                        }
                    });
        } else {
            renderLocalAIPerformanceReport(test, correct, total, language);
        }
    }

    private String generateQuestionTrick(String qText, String correctAns, String exp, boolean isHindi) {
        if (exp != null && !exp.trim().isEmpty()) {
            String expLower = exp.toLowerCase();
            if (expLower.contains("trick") || expLower.contains("shortcut") || expLower.contains("ट्रिक") || expLower.contains("शॉर्टकट")) {
                return exp;
            }
        }

        String coreKeyword = correctAns;
        if (correctAns.contains(":")) {
            coreKeyword = correctAns.substring(correctAns.indexOf(":") + 1).trim();
        }

        if (isHindi) {
            return "याद रखने की स्मार्ट ट्रिक: **" + qText + "** → उत्तर हमेशा **" + coreKeyword + "** से संबंधित होता है। प्रश्न के मुख्य शब्दों (Keywords) को सीधे '" + coreKeyword + "' के साथ जोड़कर याद रखें!";
        } else {
            return "Speed Trick / Memory Rule: **" + qText + "** → Link key question trigger directly to **" + coreKeyword + "** for instant recall in exams!";
        }
    }

    private void renderLocalAIPerformanceReport(Test test, int correct, int total, String language) {
        if (aiLoading != null) aiLoading.setVisibility(View.GONE);

        int accuracy = total > 0 ? (correct * 100) / total : 0;
        int wrong = Math.max(0, total - correct);
        boolean isHindi = "Hindi".equalsIgnoreCase(language);
        String testTitle = (test != null && test.getTitle() != null && !test.getTitle().isEmpty()) ? test.getTitle() : "Practice Assessment";

        List<Question> questions = (test != null && test.getQuestions() != null) ? test.getQuestions() : new ArrayList<>();

        StringBuilder report = new StringBuilder();

        if (isHindi) {
            report.append("### 📊 Overall Performance & Deep Summary\n");
            report.append("आपने **").append(testTitle).append("** में **").append(correct).append("/").append(total)
                    .append("** अंक प्राप्त किए हैं (सटीकता: **").append(accuracy).append("%**)।\n\n");

            if (accuracy >= 80) {
                report.append("🌟 **उत्कृष्ट प्रदर्शन!** आपकी अवधारणाएँ अत्यंत स्पष्ट हैं।\n\n");
            } else if (accuracy >= 50) {
                report.append("👍 **अच्छा प्रयास!** बुनियादी पकड़ अच्छी है, प्रत्येक प्रश्न के ट्रिक्स से स्कोर और बढ़ेगा।\n\n");
            } else {
                report.append("🎯 **सुधार की आवश्यकता!** नीचे दिए गए एक-एक प्रश्न के गहन विश्लेषण व ट्रिक्स का अध्ययन करें।\n\n");
            }

            report.append("### 📝 Question-by-Question Deep Analysis & Tricks (प्रत्येक प्रश्न का विश्लेषण व ट्रिक)\n\n");

            if (!questions.isEmpty()) {
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    if (q == null) continue;

                    Integer selectedIdx = q.getSelectedOptionIndex();
                    boolean isCorrect = selectedIdx != null && Objects.equals(selectedIdx, q.getCorrectOptionIndex());
                    boolean isSkipped = selectedIdx == null;

                    String statusTag = isCorrect ? "🟢 CORRECT" : (isSkipped ? "⚪ SKIPPED" : "🔴 INCORRECT");

                    String qText = q.getQuestionTextForLanguage("Hindi");
                    if (qText == null || qText.trim().isEmpty()) {
                        qText = q.getQuestionText() != null ? q.getQuestionText() : "";
                    }
                    qText = qText.replaceAll("<[^>]*>", "").trim();

                    List<String> options = q.getOptionsForLanguage("Hindi");
                    if (options == null || options.isEmpty()) options = q.getOptions();

                    String yourAns = "Not Attempted (Skipped)";
                    if (selectedIdx != null && options != null && selectedIdx >= 0 && selectedIdx < options.size()) {
                        yourAns = "Option " + (char) ('A' + selectedIdx) + ": " + options.get(selectedIdx);
                    }

                    String correctAns = "N/A";
                    int correctIdx = q.getCorrectOptionIndex();
                    if (options != null && correctIdx >= 0 && correctIdx < options.size()) {
                        correctAns = "Option " + (char) ('A' + correctIdx) + ": " + options.get(correctIdx);
                    } else if (options != null && correctIdx - 1 >= 0 && correctIdx - 1 < options.size()) {
                        correctAns = "Option " + (char) ('A' + (correctIdx - 1)) + ": " + options.get(correctIdx - 1);
                    }

                    String exp = q.getExplanationForLanguage("Hindi");
                    if (exp == null || exp.trim().isEmpty()) {
                        exp = q.getExplanation() != null ? q.getExplanation() : "";
                    }
                    exp = exp.replaceAll("<[^>]*>", "").trim();

                    report.append("#### Q").append(i + 1).append(": ").append(qText).append(" - [").append(statusTag).append("]\n");
                    report.append("• **आपका उत्तर**: ").append(yourAns).append("\n");
                    report.append("• **सही उत्तर**: ").append(correctAns).append("\n");

                    report.append("• **गहन व्याख्या (Deep Explanation)**: ");
                    if (!exp.isEmpty()) {
                        report.append(exp).append("\n");
                    } else if (isCorrect) {
                        report.append("आपका चुना गया उत्तर बिल्कुल सही और तार्किक है। विकल्प '").append(correctAns).append("' इस प्रश्न का सही समाधान प्रस्तुत करता है।\n");
                    } else {
                        report.append("सही विकल्प '").append(correctAns).append("' है। विकल्पों को ध्यान से पढ़कर बुनियादी नियम को लागू करें।\n");
                    }

                    report.append("• **💡 स्मार्ट ट्रिक / याद रखने का तरीका**: ");
                    String trick = generateQuestionTrick(qText, correctAns, exp, true);
                    report.append(trick).append("\n\n");
                }
            } else {
                report.append("कोई प्रश्न विवरण उपलब्ध नहीं है।\n\n");
            }

            report.append("### 🔴 Weak Topics & Revision Strategy\n");
            report.append("• **कुल गलत/छूटे प्रश्न**: **").append(wrong).append("** प्रश्न।\n");
            report.append("• **सुझाव**: ऊपर सूचीबद्ध गलत/छूटे प्रश्नों की व्याख्या और ट्रिक्स को ध्यान से पढ़ें और मुख्य सूत्रों के नोट्स बनाएं।\n\n");

            report.append("### ⚡ Exam Shortcuts & Time Saving Tips\n");
            report.append("1. **Elimination Technique**: 4 में से 2 स्पष्ट गलत विकल्पों को पहले ही हटाएँ।\n");
            report.append("2. **Keyword Association**: प्रश्न के मुख्य शब्दों (Keywords) को उत्तर के साथ जोड़कर याद रखें।\n");
            report.append("3. **24-Hour Retake**: इस टेस्ट का 24 घंटे में पुनः प्रयास करें ताकि ट्रिक्स स्थायी याद हो जाएं।\n");

        } else {
            report.append("### 📊 Overall Performance & Deep Summary\n");
            report.append("You scored **").append(correct).append("/").append(total)
                    .append("** in **").append(testTitle).append("** with an accuracy of **").append(accuracy).append("%**.\n\n");

            if (accuracy >= 80) {
                report.append("🌟 **Outstanding Performance!** Exceptional grasp of key concepts.\n\n");
            } else if (accuracy >= 50) {
                report.append("👍 **Good Effort!** Solid foundation, review missed questions and tricks to reach 100%.\n\n");
            } else {
                report.append("🎯 **Focus Required!** Carefully study the question-by-question breakdown below.\n\n");
            }

            report.append("### 📝 Question-by-Question Deep Analysis & Tricks\n\n");

            if (!questions.isEmpty()) {
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    if (q == null) continue;

                    Integer selectedIdx = q.getSelectedOptionIndex();
                    boolean isCorrect = selectedIdx != null && Objects.equals(selectedIdx, q.getCorrectOptionIndex());
                    boolean isSkipped = selectedIdx == null;

                    String statusTag = isCorrect ? "🟢 CORRECT" : (isSkipped ? "⚪ SKIPPED" : "🔴 INCORRECT");

                    String qText = q.getQuestionTextForLanguage("English");
                    if (qText == null || qText.trim().isEmpty()) {
                        qText = q.getQuestionText() != null ? q.getQuestionText() : "";
                    }
                    qText = qText.replaceAll("<[^>]*>", "").trim();

                    List<String> options = q.getOptionsForLanguage("English");
                    if (options == null || options.isEmpty()) options = q.getOptions();

                    String yourAns = "Not Attempted (Skipped)";
                    if (selectedIdx != null && options != null && selectedIdx >= 0 && selectedIdx < options.size()) {
                        yourAns = "Option " + (char) ('A' + selectedIdx) + ": " + options.get(selectedIdx);
                    }

                    String correctAns = "N/A";
                    int correctIdx = q.getCorrectOptionIndex();
                    if (options != null && correctIdx >= 0 && correctIdx < options.size()) {
                        correctAns = "Option " + (char) ('A' + correctIdx) + ": " + options.get(correctIdx);
                    } else if (options != null && correctIdx - 1 >= 0 && correctIdx - 1 < options.size()) {
                        correctAns = "Option " + (char) ('A' + (correctIdx - 1)) + ": " + options.get(correctIdx - 1);
                    }

                    String exp = q.getExplanationForLanguage("English");
                    if (exp == null || exp.trim().isEmpty()) {
                        exp = q.getExplanation() != null ? q.getExplanation() : "";
                    }
                    exp = exp.replaceAll("<[^>]*>", "").trim();

                    report.append("#### Q").append(i + 1).append(": ").append(qText).append(" - [").append(statusTag).append("]\n");
                    report.append("• **Your Answer**: ").append(yourAns).append("\n");
                    report.append("• **Correct Answer**: ").append(correctAns).append("\n");

                    report.append("• **Deep Explanation**: ");
                    if (!exp.isEmpty()) {
                        report.append(exp).append("\n");
                    } else if (isCorrect) {
                        report.append("Your chosen answer is logically sound and accurate. Option '").append(correctAns).append("' correctly solves this problem.\n");
                    } else {
                        report.append("Correct choice is '").append(correctAns).append("'. Apply core formula/rule elimination when attempting.\n");
                    }

                    report.append("• **💡 Smart Trick & Shortcut**: ");
                    String trick = generateQuestionTrick(qText, correctAns, exp, false);
                    report.append(trick).append("\n\n");
                }
            } else {
                report.append("No question details available.\n\n");
            }

            report.append("### 🔴 Weak Topics & Revision Strategy\n");
            report.append("• **Total Missed/Skipped**: **").append(wrong).append("** questions.\n");
            report.append("• **Action**: Review the explanations and smart tricks above carefully.\n\n");

            report.append("### ⚡ Exam Shortcuts & Time Saving Tips\n");
            report.append("1. **Option Elimination**: Cross off 2 obviously incorrect choices immediately.\n");
            report.append("2. **Keyword Association**: Associate key subject terms directly with correct answers.\n");
            report.append("3. **24-Hour Retake**: Re-test yourself within 24 hours to cement shortcuts into memory.\n");
        }

        if (tvAIAnalysis != null) {
            markwon.setMarkdown(tvAIAnalysis, report.toString());
        }
    }
}
