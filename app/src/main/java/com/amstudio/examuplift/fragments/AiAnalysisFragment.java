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
    private TextView tvAIPillScore;
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
        tvAIPillScore = view.findViewById(R.id.tvAIPillScore);
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
        if (tvAIPillScore != null) {
            tvAIPillScore.setText(String.format(Locale.getDefault(), "🏆 %d/%d Correct", correct, total));
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
            tvAILoadingStatus.setText(isHindi ? "AI विशेषज्ञ ट्यूटर मॉक टेस्ट डेटा का 7-सेक्शन विश्लेषण तैयार कर रहा है..." : "AI Personal Tutor generating complete 7-section diagnostic report...");
        }

        // Build item-by-item raw performance data for AI analysis
        StringBuilder performanceData = new StringBuilder();
        String testTitle = (test != null && test.getTitle() != null) ? test.getTitle() : "Practice Assessment";
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

                String status = isCorrect ? "CORRECT ✓" : (isSkipped ? "SKIPPED/UNATTEMPTED ⚪" : "INCORRECT ✗");

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

                performanceData.append("Q").append(i + 1).append(" [").append(status).append("]:\n");
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
            prompt = "ROLE:\n" +
                    "You are an expert competitive-exam mock test analyst and personal tutor for SSC, Railway, Banking, Defence and other government examinations.\n" +
                    "Analyze the student's actual mock-test performance data provided below and generate a complete, personalized performance report in Hindi/Hinglish.\n\n" +
                    performanceData + "\n\n" +
                    "RULE 1: USE ONLY THE PROVIDED DATA\n" +
                    "- Read the complete payload, including questions, options, selected answers, correct answers, explanations, marks and question status.\n" +
                    "- Never invent questions, answers, scores, topics or explanations.\n" +
                    "- Use pre-existing question explanations and solutions wherever available.\n" +
                    "- Distinguish between skipped, incorrect attempted and correct attempted questions.\n\n" +
                    "SECTION 1: OVERALL PERFORMANCE OVERVIEW\n" +
                    "1. Total questions\n" +
                    "2. Attempted questions\n" +
                    "3. Correct answers\n" +
                    "4. Incorrect answers\n" +
                    "5. Skipped/unattempted questions\n" +
                    "6. Accuracy (calculation basis stated)\n" +
                    "7. Total score and maximum marks\n" +
                    "8. Subject-wise performance\n" +
                    "9. Key strengths and biggest improvement opportunities\n" +
                    "10. Short encouraging summary\n\n" +
                    "SECTION 2: SUBJECT-WISE PERFORMANCE\n" +
                    "- Total questions, correct, incorrect, skipped, accuracy, strong/weak topics, specific question patterns.\n\n" +
                    "SECTION 3: STRONG TOPICS AND QUESTION TYPES\n" +
                    "- Identify specific question patterns/subtypes (e.g. Maths → Percentage → Population Growth).\n" +
                    "- Correct/attempted count, total, accuracy, evidence, what student is doing well, recommendation.\n\n" +
                    "SECTION 4: WEAK TOPICS AND ERROR ANALYSIS\n" +
                    "- Subject → Topic → Specific question type, counts, accuracy, question IDs, likely difficulty cause, recommended concept revision, practice recommendation.\n\n" +
                    "SECTION 5: QUICK TIPS AND ACTION PLAN\n" +
                    "- Formulas and shortcuts, common traps, revision priorities, practical next-study-session plan.\n\n" +
                    "SECTION 6: COMPLETE QUESTION-WISE DISCUSSION\n" +
                    "Discuss EVERY question without omitting any question, grouped strictly in this order:\n" +
                    "A. SKIPPED / UNATTEMPTED QUESTIONS\n" +
                    "B. INCORRECT ATTEMPTED QUESTIONS\n" +
                    "C. CORRECT ATTEMPTED QUESTIONS\n" +
                    "For EVERY question display: Q ID, Subject/topic, Full question text, Options, Selected answer, Correct answer, Result, Solution, Step-by-step explanation, Shortcut/trick, Key learning point.\n\n" +
                    "SECTION 7: FINAL PRIORITY LIST\n" +
                    "- Top 3 topics to revise first, Top 3 question types to practise, Strong areas to maintain, Recommendation for next mock test.\n\n" +
                    "Generate the output in clear, student-friendly Markdown using tables for summary statistics where appropriate.";
        } else {
            prompt = "ROLE:\n" +
                    "You are an expert competitive-exam mock test analyst and personal tutor for SSC, Railway, Banking, Defence and other government examinations.\n" +
                    "Analyze the student's actual mock-test performance data provided below and generate a complete, personalized performance report.\n\n" +
                    performanceData + "\n\n" +
                    "RULE 1: USE ONLY THE PROVIDED DATA\n" +
                    "- Read the complete payload, including questions, options, selected answers, correct answers, explanations, marks and question status.\n" +
                    "- Never invent questions, answers, scores, topics or explanations.\n" +
                    "- Use pre-existing question explanations and solutions wherever available.\n" +
                    "- Distinguish between skipped, incorrect attempted and correct attempted questions.\n\n" +
                    "SECTION 1: OVERALL PERFORMANCE OVERVIEW\n" +
                    "Show:\n" +
                    "1. Total questions.\n" +
                    "2. Attempted questions.\n" +
                    "3. Correct answers.\n" +
                    "4. Incorrect answers.\n" +
                    "5. Skipped/unattempted questions.\n" +
                    "6. Accuracy, with its calculation basis clearly stated.\n" +
                    "7. Total score and maximum marks.\n" +
                    "8. Subject-wise performance.\n" +
                    "9. Key strengths and biggest improvement opportunities.\n" +
                    "10. A short, encouraging summary of the student's performance.\n\n" +
                    "SECTION 2: SUBJECT-WISE PERFORMANCE\n" +
                    "For each subject show: Total questions, correct, incorrect, skipped, accuracy, strong/weak topics, specific question patterns.\n\n" +
                    "SECTION 3: STRONG TOPICS AND QUESTION TYPES\n" +
                    "Identify strong topics using specific question patterns/subtypes (e.g. Maths → Percentage → Population Growth).\n" +
                    "Display: Topic and type, correct/attempted count, total questions, accuracy, evidence, what student is doing well, recommendation.\n\n" +
                    "SECTION 4: WEAK TOPICS AND ERROR ANALYSIS\n" +
                    "Display: Subject → Topic → Specific question type, counts, accuracy, related question IDs, likely difficulty cause, recommended concept revision, practice recommendation.\n\n" +
                    "SECTION 5: QUICK TIPS AND ACTION PLAN\n" +
                    "Personalized tips: relevant formulas & shortcuts, common traps, specific weak types to practise, revision priorities, practical next-study-session plan.\n\n" +
                    "SECTION 6: COMPLETE QUESTION-WISE DISCUSSION\n" +
                    "Discuss EVERY question in the test without omitting any question, grouped strictly in this exact order:\n" +
                    "A. SKIPPED / UNATTEMPTED QUESTIONS\n" +
                    "B. INCORRECT ATTEMPTED QUESTIONS\n" +
                    "C. CORRECT ATTEMPTED QUESTIONS\n" +
                    "For every question display: Question number/ID, Subject & topic, Full question text, All options, Student's choice, Correct answer, Result/status, Existing explanation/solution, Step-by-step explanation, Shortcut or trick, Key learning point.\n\n" +
                    "SECTION 7: FINAL PRIORITY LIST\n" +
                    "Finish with: Top 3 topics to revise first, Top 3 question types to practise, Strong areas to maintain, Short personalized recommendation for next mock test.\n\n" +
                    "Format the output strictly using clean Markdown with tables where appropriate.";
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
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "OpenRouter AI से विस्तृत 7-सेक्शन विश्लेषण तैयार हो रहा है..." : "Generating 7-section tutor report with OpenRouter AI...");
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
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "Pollinations AI से ट्यूटर रिपोर्ट जनरेट हो रही है..." : "Generating tutor report with Pollinations AI...");
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
        int unattempted = 0;
        int incorrect = Math.max(0, total - correct - unattempted);
        boolean isHindi = "Hindi".equalsIgnoreCase(language);
        String testTitle = (test != null && test.getTitle() != null && !test.getTitle().isEmpty()) ? test.getTitle() : "Practice Assessment";

        List<Question> questions = (test != null && test.getQuestions() != null) ? test.getQuestions() : new ArrayList<>();
        List<Question> skippedList = new ArrayList<>();
        List<Question> incorrectList = new ArrayList<>();
        List<Question> correctList = new ArrayList<>();

        for (Question q : questions) {
            if (q == null) continue;
            Integer sel = q.getSelectedOptionIndex();
            if (sel == null) {
                unattempted++;
                skippedList.add(q);
            } else if (Objects.equals(sel, q.getCorrectOptionIndex())) {
                correctList.add(q);
            } else {
                incorrectList.add(q);
            }
        }
        incorrect = incorrectList.size();

        StringBuilder report = new StringBuilder();

        if (isHindi) {
            report.append("### SECTION 1: OVERALL PERFORMANCE OVERVIEW\n\n");
            report.append("| Metric | Value |\n|---|---|\n");
            report.append("| Total Questions | ").append(total).append(" |\n");
            report.append("| Attempted Questions | ").append(correct + incorrect).append(" |\n");
            report.append("| Correct Answers | ").append(correct).append(" |\n");
            report.append("| Incorrect Answers | ").append(incorrect).append(" |\n");
            report.append("| Skipped / Unattempted | ").append(unattempted).append(" |\n");
            report.append("| Accuracy | ").append(accuracy).append("% (Basis: Correct / Attempted) |\n");
            report.append("| Score | ").append(correct).append(" / ").append(total).append(" |\n\n");

            report.append("• **Key Strengths**: Bounded reasoning & foundational subject recall.\n");
            report.append("• **Improvement Opportunities**: Targeted concept revision on missed questions.\n");
            report.append("• **Summary**: Solid effort! Focused revision of weak question types will boost overall accuracy.\n\n");

            report.append("### SECTION 2: SUBJECT-WISE PERFORMANCE\n\n");
            report.append("**Subject**: ").append(testTitle).append("\n");
            report.append("• Total: ").append(total).append(" | Correct: ").append(correct).append(" | Incorrect: ").append(incorrect).append(" | Skipped: ").append(unattempted).append(" | Accuracy: ").append(accuracy).append("%\n\n");

            report.append("### SECTION 3: STRONG TOPICS AND QUESTION TYPES\n\n");
            if (!correctList.isEmpty()) {
                for (Question q : correctList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "Hindi");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append("): Correctly identified core question pattern and applied rules.\n");
                }
            } else {
                report.append("• Revision recommended to build strong topics.\n");
            }
            report.append("\n");

            report.append("### SECTION 4: WEAK TOPICS AND ERROR ANALYSIS\n\n");
            if (!incorrectList.isEmpty() || !skippedList.isEmpty()) {
                for (Question q : incorrectList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "Hindi");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append(" - Incorrect): Conceptual step or formula calculation trigger missed.\n");
                }
                for (Question q : skippedList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "Hindi");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append(" - Skipped): Time management or unvisited question.\n");
                }
            } else {
                report.append("• Zero weak areas identified! Outstanding performance.\n");
            }
            report.append("\n");

            report.append("### SECTION 5: QUICK TIPS AND ACTION PLAN\n\n");
            report.append("1. **Shortcut & Formula**: Set up equations directly before calculating.\n");
            report.append("2. **Option Elimination**: Cross off 2 obviously wrong options immediately.\n");
            report.append("3. **24-Hour Rule**: Re-attempt all skipped and incorrect questions within 24 hours.\n\n");

            report.append("### SECTION 6: COMPLETE QUESTION-WISE DISCUSSION\n\n");
            report.append("#### A. SKIPPED / UNATTEMPTED QUESTIONS\n");
            if (skippedList.isEmpty()) {
                report.append("No questions skipped.\n\n");
            } else {
                for (Question q : skippedList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "Hindi");
                }
            }

            report.append("#### B. INCORRECT ATTEMPTED QUESTIONS\n");
            if (incorrectList.isEmpty()) {
                report.append("No incorrect questions.\n\n");
            } else {
                for (Question q : incorrectList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "Hindi");
                }
            }

            report.append("#### C. CORRECT ATTEMPTED QUESTIONS\n");
            if (correctList.isEmpty()) {
                report.append("No correct questions.\n\n");
            } else {
                for (Question q : correctList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "Hindi");
                }
            }

            report.append("### SECTION 7: FINAL PRIORITY LIST\n\n");
            report.append("1. **Top 3 Revision Topics**: ");
            if (!incorrectList.isEmpty()) {
                report.append(deduceStandardSyllabusTopic(incorrectList.get(0), "Hindi"));
            } else {
                report.append("Core Formulas & Short Tricks");
            }
            report.append("\n2. **Practice Strategy**: Daily 20-minute timed drills on missed topics.\n");
            report.append("3. **Next Test Goal**: Maintain 85%+ accuracy by eliminating guesswork.\n");

        } else {
            report.append("### SECTION 1: OVERALL PERFORMANCE OVERVIEW\n\n");
            report.append("| Metric | Value |\n|---|---|\n");
            report.append("| Total Questions | ").append(total).append(" |\n");
            report.append("| Attempted Questions | ").append(correct + incorrect).append(" |\n");
            report.append("| Correct Answers | ").append(correct).append(" |\n");
            report.append("| Incorrect Answers | ").append(incorrect).append(" |\n");
            report.append("| Skipped / Unattempted | ").append(unattempted).append(" |\n");
            report.append("| Accuracy | ").append(accuracy).append("% (Basis: Correct / Attempted) |\n");
            report.append("| Score | ").append(correct).append(" / ").append(total).append(" |\n\n");

            report.append("• **Key Strengths**: Bounded reasoning & foundational subject recall.\n");
            report.append("• **Improvement Opportunities**: Targeted concept revision on missed questions.\n");
            report.append("• **Summary**: Solid effort! Focused revision of weak question types will boost overall accuracy.\n\n");

            report.append("### SECTION 2: SUBJECT-WISE PERFORMANCE\n\n");
            report.append("**Subject**: ").append(testTitle).append("\n");
            report.append("• Total: ").append(total).append(" | Correct: ").append(correct).append(" | Incorrect: ").append(incorrect).append(" | Skipped: ").append(unattempted).append(" | Accuracy: ").append(accuracy).append("%\n\n");

            report.append("### SECTION 3: STRONG TOPICS AND QUESTION TYPES\n\n");
            if (!correctList.isEmpty()) {
                for (Question q : correctList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "English");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append("): Correctly identified core question pattern and applied rules.\n");
                }
            } else {
                report.append("• Revision recommended to build strong topics.\n");
            }
            report.append("\n");

            report.append("### SECTION 4: WEAK TOPICS AND ERROR ANALYSIS\n\n");
            if (!incorrectList.isEmpty() || !skippedList.isEmpty()) {
                for (Question q : incorrectList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "English");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append(" - Incorrect): Conceptual step or formula calculation trigger missed.\n");
                }
                for (Question q : skippedList) {
                    int qIdx = questions.indexOf(q) + 1;
                    String topic = deduceStandardSyllabusTopic(q, "English");
                    report.append("• **").append(topic).append("** (Q").append(qIdx).append(" - Skipped): Time management or unvisited question.\n");
                }
            } else {
                report.append("• Zero weak areas identified! Outstanding performance.\n");
            }
            report.append("\n");

            report.append("### SECTION 5: QUICK TIPS AND ACTION PLAN\n\n");
            report.append("1. **Shortcut & Formula**: Set up equations directly before calculating.\n");
            report.append("2. **Option Elimination**: Cross off 2 obviously wrong options immediately.\n");
            report.append("3. **24-Hour Rule**: Re-attempt all skipped and incorrect questions within 24 hours.\n\n");

            report.append("### SECTION 6: COMPLETE QUESTION-WISE DISCUSSION\n\n");
            report.append("#### A. SKIPPED / UNATTEMPTED QUESTIONS\n");
            if (skippedList.isEmpty()) {
                report.append("No questions skipped.\n\n");
            } else {
                for (Question q : skippedList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "English");
                }
            }

            report.append("#### B. INCORRECT ATTEMPTED QUESTIONS\n");
            if (incorrectList.isEmpty()) {
                report.append("No incorrect questions.\n\n");
            } else {
                for (Question q : incorrectList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "English");
                }
            }

            report.append("#### C. CORRECT ATTEMPTED QUESTIONS\n");
            if (correctList.isEmpty()) {
                report.append("No correct questions.\n\n");
            } else {
                for (Question q : correctList) {
                    appendQuestionDiscussion(report, q, questions.indexOf(q) + 1, "English");
                }
            }

            report.append("### SECTION 7: FINAL PRIORITY LIST\n\n");
            report.append("1. **Top 3 Revision Topics**: ");
            if (!incorrectList.isEmpty()) {
                report.append(deduceStandardSyllabusTopic(incorrectList.get(0), "English"));
            } else {
                report.append("Core Formulas & Short Tricks");
            }
            report.append("\n2. **Practice Strategy**: Daily 20-minute timed drills on missed topics.\n");
            report.append("3. **Next Test Goal**: Maintain 85%+ accuracy by eliminating guesswork.\n");
        }

        if (tvAIAnalysis != null) {
            markwon.setMarkdown(tvAIAnalysis, report.toString());
        }
    }

    private void appendQuestionDiscussion(StringBuilder sb, Question q, int qIdx, String lang) {
        if (q == null) return;
        String topic = deduceStandardSyllabusTopic(q, lang);
        String qText = q.getQuestionTextForLanguage(lang);
        if (qText == null || qText.trim().isEmpty()) qText = q.getQuestionText() != null ? q.getQuestionText() : "";
        qText = qText.replaceAll("<[^>]*>", "").trim();

        List<String> options = q.getOptionsForLanguage(lang);
        if (options == null || options.isEmpty()) options = q.getOptions();

        Integer sel = q.getSelectedOptionIndex();
        String studentAns = "Not Attempted";
        if (sel != null && options != null && sel >= 0 && sel < options.size()) {
            studentAns = "Option " + (char) ('A' + sel) + ": " + options.get(sel);
        }

        int correctIdx = q.getCorrectOptionIndex();
        String correctAns = "N/A";
        if (options != null && correctIdx >= 0 && correctIdx < options.size()) {
            correctAns = "Option " + (char) ('A' + correctIdx) + ": " + options.get(correctIdx);
        } else if (options != null && correctIdx - 1 >= 0 && correctIdx - 1 < options.size()) {
            correctAns = "Option " + (char) ('A' + (correctIdx - 1)) + ": " + options.get(correctIdx - 1);
        }

        String status = sel == null ? "Skipped" : (Objects.equals(sel, q.getCorrectOptionIndex()) ? "Correct ✓" : "Incorrect ✗");

        String exp = q.getExplanationForLanguage(lang);
        if (exp == null || exp.trim().isEmpty()) exp = q.getExplanation() != null ? q.getExplanation() : "";
        exp = exp.replaceAll("<[^>]*>", "").trim();

        sb.append("**Question ").append(qIdx).append(" (ID: Q").append(qIdx).append(")** | ").append(topic).append("\n");
        sb.append("• **Question**: ").append(qText).append("\n");
        if (options != null && !options.isEmpty()) {
            sb.append("• **Options**: ");
            for (int i = 0; i < options.size(); i++) {
                sb.append("(").append((char) ('A' + i)).append(") ").append(options.get(i)).append(" ");
            }
            sb.append("\n");
        }
        sb.append("• **Student Choice**: ").append(studentAns).append("\n");
        sb.append("• **Correct Answer**: ").append(correctAns).append("\n");
        sb.append("• **Status**: ").append(status).append("\n");
        if (!exp.isEmpty()) {
            sb.append("• **Solution & Explanation**: ").append(exp).append("\n");
        }
        sb.append("• **Key Learning**: Focus on direct formula setup and option elimination.\n\n");
    }

    private String deduceStandardSyllabusTopic(Question q, String defaultLang) {
        if (q == null) return "General Aptitude";
        String text = (q.getQuestionTextForLanguage(defaultLang) + " " + q.getExplanationForLanguage(defaultLang)).toLowerCase(Locale.ROOT);

        if (text.contains("lcm") || text.contains("hcf") || text.contains("factor") || text.contains("multiple")) return "LCM & HCF";
        if (text.contains("average") || text.contains("mean") || text.contains("औसत")) return "Averages";
        if (text.contains("ratio") || text.contains("proportion") || text.contains("अनुपात")) return "Ratio & Proportion";
        if (text.contains("percent") || text.contains("%") || text.contains("प्रतिशत")) return "Percentages";
        if (text.contains("profit") || text.contains("loss") || text.contains("cost price") || text.contains("selling price") || text.contains("लाभ") || text.contains("हानि")) return "Profit & Loss";
        if (text.contains("interest") || text.contains("compound") || text.contains("simple interest") || text.contains("ब्याज")) return "Simple & Compound Interest";
        if (text.contains("work") || text.contains("worker") || text.contains("days") || text.contains("कार्य") || text.contains("समय")) return "Time & Work";
        if (text.contains("speed") || text.contains("distance") || text.contains("train") || text.contains("boat") || text.contains("गति") || text.contains("दूरी")) return "Speed, Time & Distance";
        if (text.contains("area") || text.contains("volume") || text.contains("surface") || text.contains("triangle") || text.contains("circle") || text.contains("cube") || text.contains("क्षेत्रफल")) return "Mensuration & Geometry";
        if (text.contains("seat") || text.contains("row") || text.contains("line") || text.contains("arrangement") || text.contains("बैठक")) return "Linear & Circular Seating Arrangement";
        if (text.contains("syllogism") || text.contains("statement") || text.contains("conclusion") || text.contains("कथन")) return "Syllogism & Logical Deduction";
        if (text.contains("blood") || text.contains("relation") || text.contains("father") || text.contains("mother") || text.contains("संबंध")) return "Blood Relations";
        if (text.contains("direction") || text.contains("north") || text.contains("south") || text.contains("east") || text.contains("west") || text.contains("दिशा")) return "Direction Sense Test";
        if (text.contains("series") || text.contains("pattern") || text.contains("missing number") || text.contains("संख्या श्रेणी")) return "Number & Alphabet Series";
        if (text.contains("graph") || text.contains("chart") || text.contains("table") || text.contains("bar") || text.contains("pie")) return "Data Interpretation";
        if (text.contains("gdp") || text.contains("bank") || text.contains("governor") || text.contains("award") || text.contains("summit") || text.contains("minister") || text.contains("current")) return "General Awareness & Current Affairs";

        return "General Knowledge & Concepts";
    }
}
