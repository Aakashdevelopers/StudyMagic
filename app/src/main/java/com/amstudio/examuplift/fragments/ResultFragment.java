package com.amstudio.examuplift.fragments;

import android.os.Bundle;
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
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.models.ai.RapidApiRequest;
import com.amstudio.examuplift.models.ai.RapidApiResponse;
import com.amstudio.examuplift.utils.Constants;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultFragment extends Fragment {

    private TextView tvAIAnalysis;
    private View aiLoading;
    private Markwon markwon;
    private KonfettiView konfettiView;
    private String selectedLanguage = "English";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        int correct = getArguments() != null ? getArguments().getInt("correct") : 0;
        int total = getArguments() != null ? getArguments().getInt("total") : 0;
        Test test = getArguments() != null ? (Test) getArguments().getSerializable("test") : null;

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
        View cvAIAnalysis = view.findViewById(R.id.cvAIAnalysis);

        boolean isMockTest = getArguments() != null && getArguments().getBoolean("isMockTest", false);

        ChipGroup chipGroupLanguage = view.findViewById(R.id.chipGroupLanguage);
        if (chipGroupLanguage != null) {
            chipGroupLanguage.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipHindi)) {
                    selectedLanguage = "Hindi";
                } else {
                    selectedLanguage = "English";
                }
                if (test != null && isMockTest) {
                    fetchAIPerformanceAnalysis(test, selectedLanguage);
                }
            });
        }

        RecyclerView rvExplanations = view.findViewById(R.id.rvExplanations);
        TextView tvIncorrectTitle = view.findViewById(R.id.tvIncorrectTitle);

        if (test != null && test.getQuestions() != null) {
            List<Question> incorrectQuestions = new ArrayList<>();
            for (Question q : test.getQuestions()) {
                if (q != null && (q.getSelectedOptionIndex() == null || q.getSelectedOptionIndex() != q.getCorrectOptionIndex())) {
                    incorrectQuestions.add(q);
                }
            }

            if (!incorrectQuestions.isEmpty()) {
                tvIncorrectTitle.setVisibility(View.VISIBLE);
                ExplanationAdapter adapter = new ExplanationAdapter(incorrectQuestions);
                rvExplanations.setAdapter(adapter);
            }

            if (isMockTest) {
                if (cvAIAnalysis != null) cvAIAnalysis.setVisibility(View.VISIBLE);
                fetchAIPerformanceAnalysis(test, selectedLanguage);
            } else {
                if (cvAIAnalysis != null) cvAIAnalysis.setVisibility(View.GONE);
            }
        } else {
            if (cvAIAnalysis != null) cvAIAnalysis.setVisibility(View.GONE);
        }

        tvScore.setText(correct + "/" + total);
        tvCorrect.setText(String.format(Locale.getDefault(), "%02d", correct));
        tvWrong.setText(String.format(Locale.getDefault(), "%02d", total - correct));
        
        int accuracy = total > 0 ? (correct * 100) / total : 0;
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

    private void fetchAIPerformanceAnalysis(Test test, String language) {
        if (Constants.RAPIDAPI_KEY == null || Constants.RAPIDAPI_KEY.isEmpty()) {
            aiLoading.setVisibility(View.GONE);
            tvAIAnalysis.setText("AI analysis is not configured for this build. Please contact support.");
            return;
        }

        if (test == null || test.getQuestions() == null) {
            aiLoading.setVisibility(View.GONE);
            return;
        }

        aiLoading.setVisibility(View.VISIBLE);
        tvAIAnalysis.setText(language.equalsIgnoreCase("Hindi") ? "आपके प्रदर्शन का विश्लेषण हो रहा है..." : "Analysing your performance...");

        StringBuilder performanceData = new StringBuilder();
        performanceData.append("Test Title: ").append(test.getTitle() != null ? test.getTitle() : "").append("\n\n");
        
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

        String prompt;
        if (language.equalsIgnoreCase("Hindi")) {
            prompt = "आप Exam Uplift के छात्रों के लिए एक विशेषज्ञ शिक्षा मेंटर और प्रदर्शन विश्लेषक हैं।\n" +
                    "निम्नलिखित परीक्षण परिणामों का विस्तार से विश्लेषण करें और मजबूत और कमजोर विषयों को स्पष्ट रूप से हाइलाइट करें।\n\n" +
                    "परीक्षण का शीर्षक: " + test.getTitle() + "\n\n" +
                    "प्रदर्शन डेटा:\n" + performanceData.toString() + "\n" +
                    "कृपया निम्नलिखित संरचना में एक विस्तृत और अत्यंत उत्साहवर्धक रिपोर्ट **पूर्णतः हिन्दी भाषा** में प्रदान करें:\n\n" +
                    "1. **📊 कुल प्रदर्शन सारांश (Overall Performance)**: स्कोर मूल्यांकन और सटीकता का सारांश।\n" +
                    "2. **🟢 मजबूत विषय (Strong Topics)**: उन सभी विषयों/अवधारणाओं की सूची बनाएं और उन्हें **बोल्ड** (`**विषय का नाम**`) में हाइलाइट करें जिनमें छात्र ने अच्छा प्रदर्शन किया।\n" +
                    "3. **🔴 कमजोर विषय (Weak Topics)**: उन सभी विषयों/अवधारणाओं की सूची बनाएं और उन्हें **बोल्ड** (`**विषय का नाम**`) में हाइलाइट करें जहाँ छात्र ने गलत उत्तर दिए या छोड़ दिए। अवधारणा को स्पष्ट करें।\n" +
                    "4. **🔍 गलतियों का विश्लेषण (Mistake Analysis)**: विश्लेषण करें कि क्या गलतियाँ वैचारिक कमजोरी या असावधानी के कारण थीं।\n" +
                    "5. **🎯 अगली परीक्षा के लिए कार्य योजना (Action Plan)**: कमजोर विषयों को मजबूत बनाने के लिए 3 व्यावहारिक सुझाव दें।\n\n" +
                    "प्रारूप नियम:\n" +
                    "- सभी विषय नामों को **बोल्ड** करें।\n" +
                    "- मजबूत विषयों के लिए (🟢) और कमजोर विषयों के लिए (🔴) संकेतकों का उपयोग करें।\n" +
                    "- पूरी रिपोर्ट स्पष्ट, छात्र-अनुकूल हिन्दी भाषा में लिखें।";
        } else {
            prompt = "You are an expert educational mentor and performance analyst for Exam Uplift students.\n" +
                    "Analyze the following test results in EXTREME DETAIL and highlight strong and weak topics clearly in English.\n\n" +
                    "Test Title: " + test.getTitle() + "\n\n" +
                    "Performance Data:\n" + performanceData.toString() + "\n" +
                    "Provide a comprehensive, highly encouraging performance report in this exact structure:\n\n" +
                    "1. **📊 Overall Performance Summary**: Score assessment and accuracy summary.\n" +
                    "2. **🟢 Strong Topics (Strong Areas)**: List and HIGHLIGHT in bold (`**topic name**`) every topic/concept where the student performed well. Explain why they are strong here.\n" +
                    "3. **🔴 Weak Topics (Weak Areas)**: List and HIGHLIGHT in bold (`**topic name**`) every topic/concept where the student answered incorrectly or skipped. Explain what concept was missed.\n" +
                    "4. **🔍 Mistake Analysis**: Analyze whether errors were due to conceptual weakness, silly mistakes, or time pressure.\n" +
                    "5. **🎯 Action Plan for Next Test**: Give 3 practical, actionable tips to convert weak topics into strong ones.\n\n" +
                    "Formatting Instructions:\n" +
                    "- Bold all topic names clearly (e.g. **Algebra**, **Grammar**, **History**).\n" +
                    "- Use green indicators (🟢) for Strong Topics and red indicators (🔴) for Weak Topics.\n" +
                    "- Write in clear, student-friendly English language with bullet points.";
        }

        List<RapidApiRequest.Message> messages = new ArrayList<>();
        messages.add(new RapidApiRequest.Message("user", prompt));

        RapidApiRequest request = new RapidApiRequest(messages);

        ApiClient.getAIInterface().getCompletion(Constants.RAPIDAPI_HOST, Constants.RAPIDAPI_KEY, request)
                .enqueue(new Callback<RapidApiResponse>() {
                    @Override
                    public void onResponse(Call<RapidApiResponse> call, Response<RapidApiResponse> response) {
                        if (!isAdded()) return;
                        aiLoading.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().result != null && !response.body().result.isEmpty()) {
                            String analysis = response.body().result;
                            markwon.setMarkdown(tvAIAnalysis, analysis);
                        } else {
                            tvAIAnalysis.setText(language.equalsIgnoreCase("Hindi") ? "इस समय विश्लेषण पूरा नहीं किया जा सका।" : "Could not complete analysis at this time.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RapidApiResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        aiLoading.setVisibility(View.GONE);
                        tvAIAnalysis.setText(language.equalsIgnoreCase("Hindi") ? "विश्लेषण के दौरान नेटवर्क त्रुटि।" : "Network error during analysis.");
                    }
                });
    }
}