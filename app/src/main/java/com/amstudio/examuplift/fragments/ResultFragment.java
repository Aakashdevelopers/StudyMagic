package com.amstudio.examuplift.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.AiChatActivity;
import com.amstudio.examuplift.MainActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.adapters.ExplanationAdapter;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.Spread;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class ResultFragment extends Fragment {

    private KonfettiView konfettiView;
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

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> navigateBackOrHome());
            if (currentTest != null && currentTest.getTitle() != null && !currentTest.getTitle().isEmpty()) {
                toolbar.setSubtitle(currentTest.getTitle());
            }
        }

        correctCount = getArguments() != null ? getArguments().getInt("correct") : 0;
        totalCount = getArguments() != null ? getArguments().getInt("total") : 0;
        currentTest = getArguments() != null ? (Test) getArguments().getSerializable("test") : null;

        TextView tvScore = view.findViewById(R.id.tvScoreValue);
        TextView tvCorrect = view.findViewById(R.id.tvCorrectCount);
        TextView tvWrong = view.findViewById(R.id.tvWrongCount);
        TextView tvAccuracy = view.findViewById(R.id.tvAccuracy);
        TextView tvMessage = view.findViewById(R.id.tvPerformanceMessage);
        CircularProgressIndicator resultRing = view.findViewById(R.id.resultRing);
        konfettiView = view.findViewById(R.id.konfettiView);

        RecyclerView rvExplanations = view.findViewById(R.id.rvExplanations);
        TextView tvIncorrectTitle = view.findViewById(R.id.tvIncorrectTitle);

        int calculatedCorrect = 0;
        int calculatedWrong = 0;

        if (currentTest != null && currentTest.getQuestions() != null) {
            totalCount = currentTest.getQuestions().size();
            List<Question> incorrectQuestions = new ArrayList<>();
            for (Question q : currentTest.getQuestions()) {
                if (q != null) {
                    if (q.getSelectedOptionIndex() != null) {
                        if (q.getSelectedOptionIndex() == q.getCorrectOptionIndex()) {
                            calculatedCorrect++;
                        } else {
                            calculatedWrong++;
                            incorrectQuestions.add(q);
                        }
                    } else {
                        incorrectQuestions.add(q);
                    }
                }
            }
            correctCount = calculatedCorrect;

            if (!incorrectQuestions.isEmpty()) {
                tvIncorrectTitle.setVisibility(View.VISIBLE);
                ExplanationAdapter adapter = new ExplanationAdapter(incorrectQuestions);
                rvExplanations.setAdapter(adapter);
            }
        } else {
            calculatedWrong = Math.max(0, totalCount - correctCount);
        }

        double positiveVal = (currentTest != null) ? currentTest.getCorrectMarks() : 1.0;
        boolean hasNeg = currentTest != null && currentTest.hasNegativeMarking();
        double negVal = hasNeg ? currentTest.getNegativeMarking() : 0.0;

        double earnedMarks = correctCount * positiveVal;
        double totalDeduction = calculatedWrong * negVal;
        double finalScore = earnedMarks - totalDeduction;

        double maxMarksVal = (currentTest != null) ? currentTest.getTotalMarks() : (totalCount * positiveVal);

        TextView tvNegInfo = view.findViewById(R.id.tvNegativeMarkingInfo);
        if (tvNegInfo != null) {
            String posStr = (positiveVal % 1 == 0) ? String.valueOf((long) positiveVal) : String.format(Locale.getDefault(), "%.2f", positiveVal);
            String negStr = (negVal % 1 == 0) ? String.valueOf((long) negVal) : String.format(Locale.getDefault(), "%.2f", negVal);
            if (hasNeg && negVal > 0) {
                tvNegInfo.setVisibility(View.VISIBLE);
                tvNegInfo.setText("Marking Scheme: +" + posStr + " per correct, -" + negStr + " per wrong");
            } else if (positiveVal != 1.0) {
                tvNegInfo.setVisibility(View.VISIBLE);
                tvNegInfo.setText("Marking Scheme: +" + posStr + " per correct answer");
            } else {
                tvNegInfo.setVisibility(View.GONE);
            }
        }

        String scoreStr = (finalScore % 1 == 0) ? String.valueOf((long) finalScore) : String.format(Locale.getDefault(), "%.2f", finalScore);
        if (scoreStr.endsWith(".00")) scoreStr = scoreStr.substring(0, scoreStr.length() - 3);
        else if (scoreStr.endsWith("0")) scoreStr = scoreStr.substring(0, scoreStr.length() - 1);

        String maxMarksStr = (maxMarksVal % 1 == 0) ? String.valueOf((long) maxMarksVal) : String.format(Locale.getDefault(), "%.2f", maxMarksVal);
        if (maxMarksStr.endsWith(".00")) maxMarksStr = maxMarksStr.substring(0, maxMarksStr.length() - 3);
        else if (maxMarksStr.endsWith("0")) maxMarksStr = maxMarksStr.substring(0, maxMarksStr.length() - 1);

        tvScore.setText(scoreStr + "/" + maxMarksStr);
        tvCorrect.setText(String.format(Locale.getDefault(), "%02d", correctCount));
        tvWrong.setText(String.format(Locale.getDefault(), "%02d", calculatedWrong));

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


        // Embed AI Analysis Fragment into Container
        if (savedInstanceState == null) {
            AiAnalysisFragment aiFragment = AiAnalysisFragment.newInstance(currentTest, correctCount, totalCount);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.aiAnalysisContainer, aiFragment)
                    .commit();
        }

        // Handle Mode Selection Chips: Overview vs AI Analysis
        ChipGroup chipGroupMode = view.findViewById(R.id.chipGroupMode);
        View cvBasicSummary = view.findViewById(R.id.cvBasicSummary);
        View aiAnalysisContainer = view.findViewById(R.id.aiAnalysisContainer);
        NestedScrollView nestedScrollView = view.findViewById(R.id.nestedScrollView);
        boolean hasIncorrect = tvIncorrectTitle.getVisibility() == View.VISIBLE;

        if (chipGroupMode != null) {
            // Set initial state: Overview selected -> hide AI analysis
            if (chipGroupMode.getCheckedChipId() == R.id.chipModeAi) {
                if (cvBasicSummary != null) cvBasicSummary.setVisibility(View.GONE);
                if (aiAnalysisContainer != null) aiAnalysisContainer.setVisibility(View.VISIBLE);
                if (tvIncorrectTitle != null) tvIncorrectTitle.setVisibility(View.GONE);
                if (rvExplanations != null) rvExplanations.setVisibility(View.GONE);
            } else {
                if (cvBasicSummary != null) cvBasicSummary.setVisibility(View.VISIBLE);
                if (aiAnalysisContainer != null) aiAnalysisContainer.setVisibility(View.GONE);
                if (tvIncorrectTitle != null && hasIncorrect) tvIncorrectTitle.setVisibility(View.VISIBLE);
                if (rvExplanations != null && hasIncorrect) rvExplanations.setVisibility(View.VISIBLE);
            }

            chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipModeAi)) {
                    // Show AI Analysis only
                    if (cvBasicSummary != null) cvBasicSummary.setVisibility(View.GONE);
                    if (aiAnalysisContainer != null) aiAnalysisContainer.setVisibility(View.VISIBLE);
                    if (tvIncorrectTitle != null) tvIncorrectTitle.setVisibility(View.GONE);
                    if (rvExplanations != null) rvExplanations.setVisibility(View.GONE);
                } else {
                    // Overview Mode: Show Basic Summary & Questions Review, Hide AI Analysis
                    if (cvBasicSummary != null) cvBasicSummary.setVisibility(View.VISIBLE);
                    if (aiAnalysisContainer != null) aiAnalysisContainer.setVisibility(View.GONE);
                    if (tvIncorrectTitle != null && hasIncorrect) tvIncorrectTitle.setVisibility(View.VISIBLE);
                    if (rvExplanations != null && hasIncorrect) rvExplanations.setVisibility(View.VISIBLE);
                }
                if (nestedScrollView != null) {
                    nestedScrollView.post(() -> nestedScrollView.smoothScrollTo(0, 0));
                }
            });
        }

        return view;
    }

    private void navigateBackOrHome() {
        if (getActivity() instanceof MainActivity) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        } else if (getActivity() != null) {
            getActivity().finish();
        }
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
}
