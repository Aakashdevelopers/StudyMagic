package com.amstudio.examuplift.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.MainActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.adapters.ExplanationAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.api.PollinationsApi;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.models.ai.RapidApiRequest;
import com.amstudio.examuplift.models.ai.RapidApiResponse;
import com.amstudio.examuplift.utils.Constants;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.noties.markwon.Markwon;
import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.Spread;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultFragment extends Fragment {

    private TextView tvAIAnalysis;
    private View aiLoading;
    private TextView tvAILoadingStatus;
    private TextView tvAIPillStatus;
    private TextView tvAIPillAccuracy;
    private View btnRefreshAI;
    private Markwon markwon;
    private KonfettiView konfettiView;
    private String selectedLanguage = "English";

    private Test currentTest;
    private int correctCount = 0;
    private int totalCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        correctCount = getArguments() != null ? getArguments().getInt("correct") : 0;
        totalCount = getArguments() != null ? getArguments().getInt("total") : 0;
        currentTest = getArguments() != null ? (Test) getArguments().getSerializable("test") : null;

        markwon = Markwon.create(requireContext());

        TextView tvScore = view.findViewById(R.id.tvScoreValue);
        TextView tvCorrect = view.findViewById(R.id.tvCorrectCount);
        TextView tvWrong = view.findViewById(R.id.tvWrongCount);
        TextView tvAccuracy = view.findViewById(R.id.tvAccuracy);
        TextView tvMessage = view.findViewById(R.id.tvPerformanceMessage);
        CircularProgressIndicator resultRing = view.findViewById(R.id.resultRing);
        Button btnHome = view.findViewById(R.id.btnBackHome);
        konfettiView = view.findViewById(R.id.konfettiView);

        tvAIAnalysis = view.findViewById(R.id.tvAIAnalysisText);
        aiLoading = view.findViewById(R.id.aiAnalysisLoading);
        tvAILoadingStatus = view.findViewById(R.id.tvAILoadingStatus);
        tvAIPillStatus = view.findViewById(R.id.tvAIPillStatus);
        tvAIPillAccuracy = view.findViewById(R.id.tvAIPillAccuracy);
        btnRefreshAI = view.findViewById(R.id.btnRefreshAI);
        View cvAIAnalysis = view.findViewById(R.id.cvAIAnalysis);

        // AI Analysis is mandatory for all test modes
        if (cvAIAnalysis != null) {
            cvAIAnalysis.setVisibility(View.VISIBLE);
        }

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

        RecyclerView rvExplanations = view.findViewById(R.id.rvExplanations);
        TextView tvIncorrectTitle = view.findViewById(R.id.tvIncorrectTitle);

        if (currentTest != null && currentTest.getQuestions() != null) {
            List<Question> incorrectQuestions = new ArrayList<>();
            for (Question q : currentTest.getQuestions()) {
                if (q != null && (q.getSelectedOptionIndex() == null || q.getSelectedOptionIndex() != q.getCorrectOptionIndex())) {
                    incorrectQuestions.add(q);
                }
            }

            if (!incorrectQuestions.isEmpty()) {
                tvIncorrectTitle.setVisibility(View.VISIBLE);
                ExplanationAdapter adapter = new ExplanationAdapter(incorrectQuestions);
                rvExplanations.setAdapter(adapter);
            }
        }

        tvScore.setText(correctCount + "/" + totalCount);
        tvCorrect.setText(String.format(Locale.getDefault(), "%02d", correctCount));
        tvWrong.setText(String.format(Locale.getDefault(), "%02d", Math.max(0, totalCount - correctCount)));

        int accuracy = totalCount > 0 ? (correctCount * 100) / totalCount : 0;
        tvAccuracy.setText(accuracy + "%");
        resultRing.setProgress(accuracy);

        if (accuracy >= 80) {
            tvMessage.setText("Excellent! You're exam ready.");
            showCelebration();
        } else if (accuracy >= 50) {
            tvMessage.setText("Good effort! Keep practicing.");
            showCelebration();
        } else {
            tvMessage.setText("Needs improvement. Try again!");
        }

        btnHome.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } else if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // Trigger mandatory AI Analysis
        fetchAIPerformanceAnalysis(currentTest, correctCount, totalCount, selectedLanguage);

        return view;
    }

    private void showCelebration() {
        EmitterConfig emitterConfig = new Emitter(5L, TimeUnit.SECONDS).perSecond(30);
        Party party = new PartyFactory(emitterConfig)
                .angle(Angle.RIGHT - 45)
                .spread(Spread.WIDE)
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(10f, 30f)
                .position(new Position.Relative(0.0, 0.5))
                .build();

        Party partyLeft = new PartyFactory(emitterConfig)
                .angle(Angle.LEFT + 45)
                .spread(Spread.WIDE)
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(10f, 30f)
                .position(new Position.Relative(1.0, 0.5))
                .build();

        konfettiView.start(Arrays.asList(party, partyLeft));
    }

    private void fetchAIPerformanceAnalysis(Test test, int correct, int total, String language) {
        int accuracy = total > 0 ? (correct * 100) / total : 0;

        // Update Quick Metrics Bar
        if (tvAIPillAccuracy != null) {
            tvAIPillAccuracy.setText("📊 " + accuracy + "% Accuracy");
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

        StringBuilder performanceData = new StringBuilder();
        String testTitle = (test != null && test.getTitle() != null) ? test.getTitle() : "Practice Quiz";
        performanceData.append("Test Title: ").append(testTitle).append("\n");
        performanceData.append("Score: ").append(correct).append("/").append(total).append(" (").append(accuracy).append("% Accuracy)\n\n");

        if (test != null && test.getQuestions() != null && !test.getQuestions().isEmpty()) {
            for (int i = 0; i < test.getQuestions().size(); i++) {
                Question q = test.getQuestions().get(i);
                if (q == null) continue;
                boolean isCorrect = q.getSelectedOptionIndex() != null && q.getSelectedOptionIndex() == q.getCorrectOptionIndex();

                String selectedOption = "Not Attempted";
                if (q.getSelectedOptionIndex() != null && q.getOptions() != null && q.getSelectedOptionIndex() >= 0 && q.getSelectedOptionIndex() < q.getOptions().size()) {
                    selectedOption = q.getOptions().get(q.getSelectedOptionIndex());
                }
                String correctOption = "N/A";
                if (q.getOptions() != null && q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < q.getOptions().size()) {
                    correctOption = q.getOptions().get(q.getCorrectOptionIndex());
                }

                performanceData.append("Q").append(i + 1).append(": ").append(q.getQuestionText() != null ? q.getQuestionText() : "")
                        .append("\n - Student Answer: ").append(selectedOption)
                        .append("\n - Correct Answer: ").append(correctOption)
                        .append("\n - Result: ").append(isCorrect ? "CORRECT ✓" : "INCORRECT ✗")
                        .append("\n\n");
            }
        }

        String prompt;
        if (language.equalsIgnoreCase("Hindi")) {
            prompt = "आप Exam Uplift के छात्रों के लिए एक उत्कृष्ट AI शिक्षा मेंटर और प्रदर्शन विश्लेषक हैं।\n" +
                    "निम्नलिखित परीक्षण परिणामों का विस्तार से विश्लेषण करें:\n\n" +
                    "परीक्षण शीर्षक: " + testTitle + "\n" +
                    "प्रदर्शन डेटा:\n" + performanceData.toString() + "\n" +
                    "कृपया निम्नलिखित प्रारूप में एक अत्यंत आकर्षक, प्रेरणादायक और विस्तृत रिपोर्ट **पूर्णतः हिन्दी भाषा** में प्रदान करें:\n\n" +
                    "1. **📊 कुल प्रदर्शन सारांश (Overall Performance)**: स्कोर 평가 और शुद्धता का विस्तृत विश्लेषण।\n" +
                    "2. **🟢 मजबूत विषय (Strong Topics)**: उन अवधारणाओं और प्रश्नों की सूची जिन्हें छात्र ने सही किया, **बोल्ड** में नाम दें।\n" +
                    "3. **🔴 कमजोर विषय (Weak Topics)**: गलत या छूटे हुए प्रश्नों के आधार पर कमजोर अवधारणाओं को **बोल्ड** में हाइलाइट करें और उनका संक्षिप्त स्पष्टीकरण दें।\n" +
                    "4. **🔍 गलतियों का विश्लेषण (Mistake Analysis)**: वैचारिक कमजोरी या असावधानी का विश्लेषण।\n" +
                    "5. **🎯 अगली परीक्षा के लिए कार्य योजना (Action Plan)**: सुधार के लिए 3 व्यावहारिक, कदम-दर-कदम सुझाव।";
        } else {
            prompt = "You are an expert AI educational mentor and performance analyst for Exam Uplift students.\n" +
                    "Analyze the following test results in detail:\n\n" +
                    "Test Title: " + testTitle + "\n" +
                    "Performance Data:\n" + performanceData.toString() + "\n" +
                    "Provide a highly encouraging, actionable report in this structured markdown format:\n\n" +
                    "1. **📊 Overall Performance Summary**: Clear assessment of score and accuracy.\n" +
                    "2. **🟢 Strong Topics (Strong Areas)**: Highlight in bold (`**Topic Name**`) all concepts where the student performed well.\n" +
                    "3. **🔴 Weak Topics (Areas for Improvement)**: Highlight in bold (`**Topic Name**`) concepts where answers were incorrect or skipped, explaining the key idea.\n" +
                    "4. **🔍 Mistake Analysis**: Breakdown whether errors stemmed from concept gaps or silly mistakes.\n" +
                    "5. **🎯 Action Plan for Next Attempt**: 3 concrete, step-by-step tips to master weak topics.";
        }

        // TIER 1: RapidAPI
        if (Constants.RAPIDAPI_KEY != null && !Constants.RAPIDAPI_KEY.isEmpty()) {
            List<RapidApiRequest.Message> messages = new ArrayList<>();
            messages.add(new RapidApiRequest.Message("user", prompt));
            RapidApiRequest request = new RapidApiRequest(messages);

            ApiClient.getAIInterface().getCompletion(Constants.RAPIDAPI_HOST, Constants.RAPIDAPI_KEY, request)
                    .enqueue(new Callback<RapidApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<RapidApiResponse> call, @NonNull Response<RapidApiResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().result)) {
                                if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                                markwon.setMarkdown(tvAIAnalysis, response.body().result);
                            } else {
                                // Tier 1 failed -> Fallback to Tier 2 (Pollinations AI)
                                tryPollinationsAI(prompt, test, correct, total, language);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<RapidApiResponse> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            // Tier 1 failed -> Fallback to Tier 2 (Pollinations AI)
                            tryPollinationsAI(prompt, test, correct, total, language);
                        }
                    });
        } else {
            // No RapidAPI key configured -> Directly try Tier 2
            tryPollinationsAI(prompt, test, correct, total, language);
        }
    }

    private void tryPollinationsAI(String prompt, Test test, int correct, int total, String language) {
        if (tvAILoadingStatus != null) {
            tvAILoadingStatus.setText(language.equalsIgnoreCase("Hindi") ? "AI स्मार्ट इंजन से जुड़ रहा है..." : "Connecting to AI smart engine...");
        }

        PollinationsApi.PollinationsRequest req = new PollinationsApi.PollinationsRequest(prompt);
        ApiClient.getPollinationsInterface().getCompletion(req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;
                if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        String result = body.string();
                        if (!TextUtils.isEmpty(result)) {
                            markwon.setMarkdown(tvAIAnalysis, result);
                            return;
                        }
                    } catch (IOException ignored) {}
                }
                // Tier 2 failed -> Fallback to Tier 3 (Smart Offline Performance Engine)
                renderLocalAIPerformanceReport(test, correct, total, language);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (aiLoading != null) aiLoading.setVisibility(View.GONE);
                // Tier 2 failed -> Fallback to Tier 3 (Smart Offline Performance Engine)
                renderLocalAIPerformanceReport(test, correct, total, language);
            }
        });
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

        markwon.setMarkdown(tvAIAnalysis, report.toString());
    }
}