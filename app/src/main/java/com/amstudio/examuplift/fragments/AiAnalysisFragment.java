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
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "AI आपके प्रदर्शन का विश्लेषण कर रहा है..." : "AI is analyzing your performance...");
        }

        // Build compact performance summary for prompt to reduce token count for sub-second speeds
        StringBuilder performanceData = new StringBuilder();
        String testTitle = (test != null && test.getTitle() != null) ? test.getTitle() : "Practice Quiz";
        performanceData.append("Test Title: ").append(testTitle).append("\n");
        performanceData.append("Score: ").append(correct).append("/").append(total).append(" (").append(accuracy).append("% Accuracy)\n\n");

        if (test != null && test.getQuestions() != null && !test.getQuestions().isEmpty()) {
            int qSize = test.getQuestions().size();
            for (int i = 0; i < qSize; i++) {
                Question q = test.getQuestions().get(i);
                if (q == null) continue;
                boolean isCorrect = q.getSelectedOptionIndex() != null && Objects.equals(q.getSelectedOptionIndex(), q.getCorrectOptionIndex());

                String qText = q.getQuestionText() != null ? q.getQuestionText() : "";
                if (qText.length() > 80) {
                    qText = qText.substring(0, 80) + "...";
                }

                performanceData.append("Q").append(i + 1).append(": ").append(qText)
                        .append(" | Result: ").append(isCorrect ? "CORRECT ✓" : "INCORRECT ✗")
                        .append("\n");
            }
        }

        String prompt;
        if (language.equalsIgnoreCase("Hindi")) {
            prompt = "आप Exam Uplift के छात्रों के लिए AI शिक्षा मेंटर हैं।\n" +
                    "परीक्षण परिणाम:\n" + performanceData + "\n" +
                    "कृपया **पूर्णतः हिन्दी भाषा** में 5 बिंदुओं की संक्षिप्त, व्यावहारिक रिपोर्ट दें:\n" +
                    "1. **📊 कुल प्रदर्शन सारांश**: संक्षिप्त विवरण\n" +
                    "2. **🟢 मजबूत विषय**: मुख्य सही अवधारणाएं\n" +
                    "3. **🔴 कमजोर विषय**: सुधार योग्य मुख्य बिंदु\n" +
                    "4. **🔍 गलतियों का विश्लेषण**: वैचारिक/असावधानी की वजह\n" +
                    "5. **🎯 कार्य योजना**: 3 तुरंत कदम";
        } else {
            prompt = "You are an AI educational mentor for Exam Uplift students.\n" +
                    "Test Results:\n" + performanceData + "\n" +
                    "Provide a concise, highly actionable 5-point markdown report:\n" +
                    "1. **📊 Overall Summary**: Brief performance score\n" +
                    "2. **🟢 Strong Topics**: Main mastered concepts\n" +
                    "3. **🔴 Weak Topics**: Concepts to improve\n" +
                    "4. **🔍 Mistake Analysis**: Reason for errors\n" +
                    "5. **🎯 Action Plan**: 3 concrete next steps";
        }

        // TIER 1: OpenRouter (Ultra Fast Model e.g. meta-llama/llama-3.2-3b-instruct:free or gemini-2.0-flash)
        if (Constants.OPENROUTER_KEY != null && !Constants.OPENROUTER_KEY.trim().isEmpty()) {
            tryOpenRouterAI(prompt, Constants.OPENROUTER_MODEL, test, correct, total, language);
        } else {
            // Tier 2: Pollinations AI with fast lightweight model (mistral)
            tryPollinationsAI(prompt, test, correct, total, language);
        }
    }

    private void tryOpenRouterAI(String prompt, String modelName, Test test, int correct, int total, String language) {
        if (tvAILoadingStatus != null) {
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "OpenRouter AI से विश्लेषण हो रहा है..." : "Analyzing with OpenRouter AI...");
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
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "AI इंजन विश्लेषण कर रहा है..." : "AI engine is analyzing...");
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

    private void renderLocalAIPerformanceReport(Test test, int correct, int total, String language) {
        if (aiLoading != null) aiLoading.setVisibility(View.GONE);

        int accuracy = total > 0 ? (correct * 100) / total : 0;
        int wrong = Math.max(0, total - correct);
        String testTitle = (test != null && test.getTitle() != null && !test.getTitle().isEmpty()) ? test.getTitle() : "Practice Assessment";

        StringBuilder report = new StringBuilder();

        if (language.equalsIgnoreCase("Hindi")) {
            report.append("### 📊 कुल प्रदर्शन सारांश (Overall Performance)\n");
            report.append("आपने **").append(testTitle).append("** में **").append(correct).append("/").append(total)
                    .append("** अंक अर्जित किए हैं (सटीकता: **").append(accuracy).append("%**)।\n\n");

            if (accuracy >= 80) {
                report.append("🌟 **उत्कृष्ट प्रदर्शन!** आपकी अवधारणाओं पर मजबूत पकड़ है। परीक्षा में उच्च स्कोर के लिए इस लय को बनाए रखें।\n\n");
            } else if (accuracy >= 50) {
                report.append("👍 **अच्छा प्रयास!** आप सही दिशा में हैं, लेकिन कुछ विषयों को और निखारने की आवश्यकता है।\n\n");
            } else {
                report.append("🎯 **सुधार की आवश्यकता!** निरंतर अभ्यास और अवधारणाओं के पुनरीक्षण से आप अपने स्कोर में बड़ा उछाल ला सकते हैं।\n\n");
            }

            report.append("### 🟢 मजबूत विषय (Strong Topics)\n");
            if (correct > 0) {
                report.append("- **सही उत्तरित प्रश्न**: आपने **").append(correct).append("** प्रश्नों का सफलतापूर्वक उत्तर दिया।\n");
                report.append("- **मजबूत क्षेत्र**: इन प्रश्नों से संबंधित बुनियादी अवधारणाओं पर आपकी कमांड अच्छी है।\n\n");
            } else {
                report.append("- बुनियादी अवधारणाओं के पुनः अध्ययन से शुरुआत करें।\n\n");
            }

            report.append("### 🔴 कमजोर विषय (Weak Topics)\n");
            if (wrong > 0) {
                report.append("- **गलत/छूटे प्रश्न**: **").append(wrong).append("** प्रश्न जिन्हें विशेष ध्यान की आवश्यकता है।\n");
                report.append("- **मुख्य ध्यान क्षेत्र**: नीचे दिए गए 'Questions Review' अनुभाग से गलत उत्तरों के स्पष्टीकरण का ध्यानपूर्वक अध्ययन करें।\n\n");
            } else {
                report.append("- **कोई कमजोर क्षेत्र नहीं!** आपने शत-प्रतिशत सटीकता हासिल की है।\n\n");
            }

            report.append("### 🔍 गलतियों का विश्लेषण (Mistake Analysis)\n");
            if (accuracy < 50) {
                report.append("- मुख्य रूप से **वैचारिक स्पष्टता (Conceptual Clarity)** और प्रश्न को ध्यान से पढ़ने की कमी पाई गई।\n");
            } else {
                report.append("- गलतियाँ संभवतः **समान विकल्पों में भ्रम** या **समय प्रबंधन** के कारण हुई हैं।\n");
            }
            report.append("\n### 🎯 अगली परीक्षा के लिए कार्य योजना (Action Plan)\n");
            report.append("1. **समीक्षा करें**: नीचे दिए गए समाधानों से गलत प्रश्नों का पुनः अभ्यास करें।\n");
            report.append("2. **रिवीजन**: कमजोर विषयों के नोट्स का 15 मिनट रिवीजन करें।\n");
            report.append("3. **पुनः प्रयास**: 24 घंटे के भीतर इसी प्रकार के मॉक टेस्ट में पुनः प्रयास करें।\n");

        } else {
            report.append("### 📊 Overall Performance Summary\n");
            report.append("You scored **").append(correct).append("/").append(total)
                    .append("** in **").append(testTitle).append("** with an accuracy of **").append(accuracy).append("%**.\n\n");

            if (accuracy >= 80) {
                report.append("🌟 **Outstanding Performance!** You demonstrate strong conceptual mastery and high precision.\n\n");
            } else if (accuracy >= 50) {
                report.append("👍 **Good Effort!** You have a solid baseline, but a few topics require targeted revision.\n\n");
            } else {
                report.append("🎯 **Focus Required!** Consistent practice and revisiting core concepts will quickly boost your score.\n\n");
            }

            report.append("### 🟢 Strong Topics\n");
            if (correct > 0) {
                report.append("- **Correct Answers**: Successfully completed **").append(correct).append("** questions.\n");
                report.append("- **Core Competency**: Strong understanding in the concepts tested in these questions.\n\n");
            } else {
                report.append("- Begin by revising foundational concepts before taking timed tests.\n\n");
            }

            report.append("### 🔴 Weak Topics & Focus Areas\n");
            if (wrong > 0) {
                report.append("- **Incorrect/Skipped**: **").append(wrong).append("** questions need attention.\n");
                report.append("- **Recommended Action**: Carefully read the detailed solutions provided in the 'Questions Review' section below.\n\n");
            } else {
                report.append("- **Zero Weak Areas!** Flawless accuracy achieved.\n\n");
            }

            report.append("### 🔍 Mistake Analysis\n");
            if (accuracy < 50) {
                report.append("- Mistakes appear primarily due to **conceptual gaps** or unfamiliar question patterns.\n");
            } else {
                report.append("- Errors appear to stem from **time pressure** or slight option confusion.\n");
            }
            report.append("\n### 🎯 Action Plan for Next Attempt\n");
            report.append("1. **Review Explanations**: Inspect the detailed solutions for all missed questions below.\n");
            report.append("2. **Targeted Revision**: Spend 15 minutes reviewing formulas/rules for weak topics.\n");
            report.append("3. **Retake Practice**: Attempt a similar test within 24 hours to reinforce learning.\n");
        }

        if (tvAIAnalysis != null) {
            markwon.setMarkdown(tvAIAnalysis, report.toString());
        }
    }
}
