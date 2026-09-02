package com.amstudio.studymagic.fragments;

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

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.adapters.ExplanationAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Question;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.models.ai.ChatRequest;
import com.amstudio.studymagic.models.ai.ChatResponse;
import com.amstudio.studymagic.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.noties.markwon.Markwon;
import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.Spread;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultFragment extends Fragment {

    private TextView tvAIAnalysis;
    private View aiLoading;
    private Markwon markwon;
    private KonfettiView konfettiView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        View header = view.findViewById(R.id.llHeader);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(header);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        int correct = getArguments() != null ? getArguments().getInt("correct") : 0;
        int total = getArguments() != null ? getArguments().getInt("total") : 0;
        Test test = getArguments() != null ? (Test) getArguments().getSerializable("test") : null;

        markwon = Markwon.create(requireContext());

        TextView tvScore = view.findViewById(R.id.tvScoreValue);
        TextView tvCorrect = view.findViewById(R.id.tvCorrectCount);
        TextView tvWrong = view.findViewById(R.id.tvWrongCount);
        TextView tvAccuracy = view.findViewById(R.id.tvAccuracy);
        TextView tvMessage = view.findViewById(R.id.tvPerformanceMessage);
        com.google.android.material.progressindicator.CircularProgressIndicator resultRing = view.findViewById(R.id.resultRing);
        Button btnHome = view.findViewById(R.id.btnBackHome);
        konfettiView = view.findViewById(R.id.konfettiView);

        tvAIAnalysis = view.findViewById(R.id.tvAIAnalysisText);
        aiLoading = view.findViewById(R.id.aiAnalysisLoading);

        RecyclerView rvExplanations = view.findViewById(R.id.rvExplanations);
        TextView tvIncorrectTitle = view.findViewById(R.id.tvIncorrectTitle);

        if (test != null) {
            List<Question> incorrectQuestions = new ArrayList<>();
            for (Question q : test.getQuestions()) {
                if (q.getSelectedOptionIndex() == null || q.getSelectedOptionIndex() != q.getCorrectOptionIndex()) {
                    incorrectQuestions.add(q);
                }
            }

            if (!incorrectQuestions.isEmpty()) {
                tvIncorrectTitle.setVisibility(View.VISIBLE);
                ExplanationAdapter adapter = new ExplanationAdapter(incorrectQuestions);
                rvExplanations.setAdapter(adapter);
            }

            fetchAIPerformanceAnalysis(test);
        }

        tvScore.setText(correct + "/" + total);
        tvCorrect.setText(String.format(java.util.Locale.getDefault(), "%02d", correct));
        tvWrong.setText(String.format(java.util.Locale.getDefault(), "%02d", total - correct));
        
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
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
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

    private void fetchAIPerformanceAnalysis(Test test) {
        if (Constants.OPENROUTER_KEY == null || Constants.OPENROUTER_KEY.isEmpty()) {
            aiLoading.setVisibility(View.GONE);
            tvAIAnalysis.setText("AI analysis is not configured for this build. Please contact support.");
            return;
        }

        aiLoading.setVisibility(View.VISIBLE);
        tvAIAnalysis.setText("Analysing your performance...");

        StringBuilder performanceData = new StringBuilder();
        performanceData.append("Test Title: ").append(test.getTitle()).append("\n\n");
        
        for (int i = 0; i < test.getQuestions().size(); i++) {
            Question q = test.getQuestions().get(i);
            boolean isCorrect = q.getSelectedOptionIndex() != null && q.getSelectedOptionIndex() == q.getCorrectOptionIndex();
            performanceData.append("Q").append(i + 1).append(": ").append(q.getQuestionText())
                    .append(" | Result: ").append(isCorrect ? "CORRECT" : "INCORRECT").append("\n");
        }

        String prompt = "You are a professional educational analyst. Analyze the following student test results with EXTREME DETAIL.\n\n" +
                "Test Title: " + test.getTitle() + "\n" +
                "Performance Data:\n" + performanceData.toString() + "\n\n" +
                "Please provide a comprehensive report in this exact order:\n" +
                "1. **Overall Performance**: A high-level summary of how the student did.\n" +
                "2. **Subject-wise Breakdown**: Identify the subjects from the questions and provide an accuracy percentage for each.\n" +
                "3. **Topic-wise Strengths**: List specific topics where the student is strong (100% correct).\n" +
                "4. **Critical Weaknesses**: Identify topics that need immediate attention (Incorrect answers).\n" +
                "5. **Mistake Analysis**: Analyze if errors seem to be conceptual (multiple wrong in one topic) or random/silly mistakes.\n" +
                "6. **Actionable Improvement Strategy**: Give a clear 3-step plan to improve for the next test.\n\n" +
                "Format the response in clean markdown with bold highlights and bullet points. Be professional and encouraging.";

        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatRequest.Message("user", prompt));

        ChatRequest request = new ChatRequest(Constants.AI_MODEL, messages);
        request.max_tokens = 1000; // Increased for high detail
        request.temperature = 0.7;

        ApiClient.getAIInterface().getCompletion("Bearer " + Constants.OPENROUTER_KEY, request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                        if (!isAdded()) return;
                        aiLoading.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                            String analysis = response.body().choices.get(0).message.content;
                            markwon.setMarkdown(tvAIAnalysis, analysis);
                        } else {
                            tvAIAnalysis.setText("Could not complete analysis at this time.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        aiLoading.setVisibility(View.GONE);
                        tvAIAnalysis.setText("Network error during analysis.");
                    }
                });
    }
}