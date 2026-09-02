package com.amstudio.studymagic;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.adapters.PaletteAdapter;
import com.amstudio.studymagic.adapters.SubjectTabAdapter;
import com.amstudio.studymagic.fragments.ResultFragment;
import com.amstudio.studymagic.models.Question;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.models.Test;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MockupQuizActivity extends AppCompatActivity {

    private SupabaseTest supabaseTest;
    private Map<String, List<Question>> subjectWiseQuestions = new LinkedHashMap<>();
    private List<String> subjects = new ArrayList<>();
    private String currentSubject;
    private int currentQuestionIndexInSubject = 0;

    private Set<String> lockedSubjects = new HashSet<>();
    private Set<String> completedSubjects = new HashSet<>();

    private TextView tvTimer, tvQuizTitle, tvQuestionNoPill, tvQuestionText, tvSubjectLabel, tvSubjectTimer;
    private LinearLayout llSubjectTimerContainer;
    private com.google.android.material.progressindicator.LinearProgressIndicator quizProgress;
    private RadioGroup rgOptions;
    private RadioButton rb1, rb2, rb3, rb4;
    private Button btnSaveNext, btnMarkNext, btnClear, btnSubmitTop;
    private ImageView btnPause, btnMenu, btnMarkForReviewStar;
    private RecyclerView rvSubjects;
    private SubjectTabAdapter subjectAdapter;
    private PaletteAdapter paletteAdapter;

    private CountDownTimer timer;
    private long timeLeftInMillis;
    private boolean isTimerRunning = false;

    private CountDownTimer subjectCountDownTimer;
    private long subjectTimeLeftInMillis;
    private boolean isSubjectTimerRunning = false;
    private boolean isSubjectTimerFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mockup_quiz);

        if (getIntent() != null && getIntent().hasExtra("supabaseTest")) {
            supabaseTest = (SupabaseTest) getIntent().getSerializableExtra("supabaseTest");
            parseQuestions();
        }

        if (subjects.isEmpty()) {
            finish();
            return;
        }

        currentSubject = subjects.get(0);
        initViews();

        if (supabaseTest.duration <= 0 && supabaseTest.subjects != null) {
            int totalMins = 0;
            for (SupabaseTest.SubjectModel sm : supabaseTest.subjects) {
                totalMins += sm.duration;
            }
            supabaseTest.duration = totalMins;
        }

        timeLeftInMillis = supabaseTest.duration * 60 * 1000L;
        startTimer();
        showQuestion(0);
    }

    private void parseQuestions() {
        Gson gson = new Gson();

        if (supabaseTest.subjects == null || supabaseTest.subjects.isEmpty()) {
            try {
                String jsonStr = (supabaseTest.questionsJson instanceof String) ?
                        (String) supabaseTest.questionsJson : gson.toJson(supabaseTest.questionsJson);
                Map<String, Object> map = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>() {}.getType());

                if (map != null && map.containsKey("subjects")) {
                    Type subjectListType = new TypeToken<List<SupabaseTest.SubjectModel>>() {}.getType();
                    supabaseTest.subjects = gson.fromJson(gson.toJson(map.get("subjects")), subjectListType);

                    if (map.containsKey("test_type")) {
                        String tType = (String) map.get("test_type");
                        supabaseTest.testType = tType;
                        if ("type2".equalsIgnoreCase(tType) || "subject_wise".equalsIgnoreCase(tType)) {
                            supabaseTest.isSubjectTimerEnabled = true;
                        } else if ("type1".equalsIgnoreCase(tType) || "universal".equalsIgnoreCase(tType)) {
                            supabaseTest.isSubjectTimerEnabled = false;
                        }
                    }
                    if (map.containsKey("is_subject_timer_enabled")) {
                        supabaseTest.isSubjectTimerEnabled = (boolean) map.get("is_subject_timer_enabled");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("type2".equalsIgnoreCase(supabaseTest.testType) || "subject_wise".equalsIgnoreCase(supabaseTest.testType)) {
            supabaseTest.isSubjectTimerEnabled = true;
        } else if ("type1".equalsIgnoreCase(supabaseTest.testType) || "universal".equalsIgnoreCase(supabaseTest.testType)) {
            supabaseTest.isSubjectTimerEnabled = false;
        }

        if (supabaseTest.subjects == null) return;

        for (SupabaseTest.SubjectModel sm : supabaseTest.subjects) {
            String jsonStr = gson.toJson(sm.questionsJson);
            Type listType = new TypeToken<List<Question>>() {}.getType();
            List<Question> qs = gson.fromJson(jsonStr, listType);
            if (qs != null && !qs.isEmpty()) {
                subjectWiseQuestions.put(sm.subjectName, qs);
                subjects.add(sm.subjectName);
            }
        }
    }

    private void initViews() {
        View main = findViewById(R.id.main);
        LinearLayout llTopBar = findViewById(R.id.llTopBar);
        LinearLayout llBottomActions = findViewById(R.id.llBottomActions);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            llTopBar.setPadding(llTopBar.getPaddingLeft(), systemBars.top, llTopBar.getPaddingRight(), llTopBar.getPaddingBottom());
            llBottomActions.setPadding(llBottomActions.getPaddingLeft(), llBottomActions.getPaddingTop(), llBottomActions.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        tvTimer = findViewById(R.id.tvTimer);
        tvQuizTitle = findViewById(R.id.tvQuizTitle);
        tvQuestionNoPill = findViewById(R.id.tvQuestionNoPill);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvSubjectLabel = findViewById(R.id.tvSubjectLabel);
        tvSubjectTimer = findViewById(R.id.tvSubjectTimer);
        llSubjectTimerContainer = findViewById(R.id.llSubjectTimerContainer);
        quizProgress = findViewById(R.id.quizProgress);
        rgOptions = findViewById(R.id.rgOptions);
        rb1 = findViewById(R.id.rbOption1);
        rb2 = findViewById(R.id.rbOption2);
        rb3 = findViewById(R.id.rbOption3);
        rb4 = findViewById(R.id.rbOption4);

        btnSaveNext = findViewById(R.id.btnSaveNext);
        btnMarkNext = findViewById(R.id.btnMarkNext);
        btnClear = findViewById(R.id.btnClear);
        btnSubmitTop = findViewById(R.id.btnSubmitTop);

        btnPause = findViewById(R.id.btnPause);
        btnMenu = findViewById(R.id.btnMenu);
        btnMarkForReviewStar = findViewById(R.id.btnMarkForReviewStar);
        rvSubjects = findViewById(R.id.rvSubjects);

        tvQuizTitle.setText(supabaseTest.title);

        subjectAdapter = new SubjectTabAdapter(subjects, (subject, isLocked) -> {
            if (isLocked) {
                showSubjectLockedDialog(subject);
                return;
            }
            if (!subject.equals(currentSubject)) {
                currentSubject = subject;
                currentQuestionIndexInSubject = 0;
                resetSubjectTimer();
                updatePalette();
                showQuestion(0);
            }
        });

        rvSubjects.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSubjects.setAdapter(subjectAdapter);

        updateSubjectLockStates();
        updatePalette();

        btnMenu.setOnClickListener(view -> showPaletteBottomSheet());

        btnPause.setOnClickListener(view -> {
            if (isTimerRunning) pauseTimer();
            else startTimer();
        });

        btnClear.setOnClickListener(view -> {
            rgOptions.clearCheck();
            getCurrentQuestions().get(currentQuestionIndexInSubject).setSelectedOptionIndex(null);
            paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
        });

        btnMarkNext.setOnClickListener(view -> {
            getCurrentQuestions().get(currentQuestionIndexInSubject).setMarkedForReview(true);
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSaveNext.setOnClickListener(view -> {
            getCurrentQuestions().get(currentQuestionIndexInSubject).setMarkedForReview(false);
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSubmitTop.setOnClickListener(view -> finishTest());

        btnMarkForReviewStar.setOnClickListener(view -> {
            boolean current = getCurrentQuestions().get(currentQuestionIndexInSubject).isMarkedForReview();
            getCurrentQuestions().get(currentQuestionIndexInSubject).setMarkedForReview(!current);
            updateInfoStripIcons();
            paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
        });
    }

    private void updateSubjectLockStates() {
        lockedSubjects.clear();
        if (supabaseTest.isSubjectTimerEnabled || isSubjectTimerRunning) {
            for (String subj : subjects) {
                if (!subj.equals(currentSubject) && !completedSubjects.contains(subj)) {
                    lockedSubjects.add(subj);
                }
            }
        }
        if (subjectAdapter != null) {
            subjectAdapter.setLockedSubjects(lockedSubjects);
            subjectAdapter.setCompletedSubjects(completedSubjects);
            subjectAdapter.setSelectedSubject(currentSubject);
        }
    }

    private void showSubjectLockedDialog(String targetSubject) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_subject_locked, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        TextView tvHindi = dialogView.findViewById(R.id.tvDialogMessageHindi);
        TextView tvEnglish = dialogView.findViewById(R.id.tvDialogMessageEnglish);
        TextView tvTimer = dialogView.findViewById(R.id.tvDialogTimer);
        View btnDismiss = dialogView.findViewById(R.id.btnDialogDismiss);

        tvTitle.setText("Subject Locked 🔒");
        tvSubtitle.setText("विषय अभी अनलॉक नहीं हुआ है");

        String hindiMsg = "आप अभी " + currentSubject + " सेक्शन में हैं। इसका समय (Timer) पूरा होने के बाद ही " + targetSubject + " अनलॉक होगा।";
        String englishMsg = "You are currently taking the " + currentSubject + " section. " + targetSubject + " will unlock automatically once the current subject timer finishes.";

        tvHindi.setText(hindiMsg);
        tvEnglish.setText(englishMsg);

        if (isSubjectTimerRunning && subjectTimeLeftInMillis > 0) {
            int minutes = (int) (subjectTimeLeftInMillis / 1000) / 60;
            int seconds = (int) (subjectTimeLeftInMillis / 1000) % 60;
            tvTimer.setText(String.format(Locale.getDefault(), "Current Timer (%s): %02d:%02d", currentSubject, minutes, seconds));
        } else if (completedSubjects.contains(targetSubject)) {
            tvTimer.setText("This section is already completed.");
        } else {
            tvTimer.setText("Complete " + currentSubject + " section first.");
        }

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updatePalette() {
        paletteAdapter = new PaletteAdapter(getCurrentQuestions(), index -> {
            currentQuestionIndexInSubject = index;
            showQuestion(index);
        });
    }

    private List<Question> getCurrentQuestions() {
        return subjectWiseQuestions.get(currentSubject);
    }

    private void showPaletteBottomSheet() {
        if (isSubjectTimerRunning) {
            showSubjectLockedDialog(currentSubject);
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_quiz_palette, null);

        RecyclerView rvPalette = view.findViewById(R.id.rvQuestionPalette);
        rvPalette.setAdapter(paletteAdapter);

        paletteAdapter.setOnItemClickListener(index -> {
            currentQuestionIndexInSubject = index;
            showQuestion(index);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnSubmitQuiz).setOnClickListener(v -> {
            dialog.dismiss();
            finishTest();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void goToNextQuestion() {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQuestionIndexInSubject < currentQs.size() - 1) {
            currentQuestionIndexInSubject++;
            showQuestion(currentQuestionIndexInSubject);
        } else {
            int subjectIndex = subjects.indexOf(currentSubject);
            if (subjectIndex < subjects.size() - 1) {
                String nextSubject = subjects.get(subjectIndex + 1);
                if (isSubjectTimerRunning) {
                    showSubjectLockedDialog(nextSubject);
                    return;
                }
                completedSubjects.add(currentSubject);
                currentSubject = nextSubject;
                currentQuestionIndexInSubject = 0;
                resetSubjectTimer();
                updateSubjectLockStates();
                updatePalette();
                showQuestion(0);
                Toast.makeText(this, "Next Subject: " + currentSubject, Toast.LENGTH_SHORT).show();
            } else {
                finishTest();
            }
        }
    }

    private void showQuestion(int index) {
        List<Question> currentQs = getCurrentQuestions();
        Question q = currentQs.get(index);
        tvQuestionNoPill.setText("Q. " + (index + 1));
        tvQuestionText.setText(q.getQuestionText());
        tvSubjectLabel.setText("Subject: " + currentSubject);

        if (index == 0 && supabaseTest.isSubjectTimerEnabled) {
            startSubjectTimerIfNeeded();
        }

        updateSubjectLockStates();
        updateProgress();
        updateInfoStripIcons();

        rb1.setText(q.getOptions().get(0));
        rb2.setText(q.getOptions().get(1));
        rb3.setText(q.getOptions().get(2));
        rb4.setText(q.getOptions().get(3));

        rgOptions.clearCheck();
        if (q.getSelectedOptionIndex() != null) {
            switch (q.getSelectedOptionIndex()) {
                case 0: rb1.setChecked(true); break;
                case 1: rb2.setChecked(true); break;
                case 2: rb3.setChecked(true); break;
                case 3: rb4.setChecked(true); break;
            }
        }

        if (index == currentQs.size() - 1 && subjects.indexOf(currentSubject) == subjects.size() - 1) {
            btnSaveNext.setText("FINISH");
        } else {
            btnSaveNext.setText("Save & Next");
        }
    }

    private void updateProgress() {
        int totalQuestions = 0;
        int attemptedCount = 0;
        for (List<Question> list : subjectWiseQuestions.values()) {
            totalQuestions += list.size();
            for (Question q : list) {
                if (q.getSelectedOptionIndex() != null) attemptedCount++;
            }
        }
        int progress = (int) (((float) attemptedCount / totalQuestions) * 100);
        quizProgress.setProgress(progress);
    }

    private void updateInfoStripIcons() {
        Question q = getCurrentQuestions().get(currentQuestionIndexInSubject);
        btnMarkForReviewStar.setColorFilter(q.isMarkedForReview() ? 0xFFFF1744 : 0xFF757575);
    }

    private void saveSelectedOption() {
        int checkedId = rgOptions.getCheckedRadioButtonId();
        List<Question> currentQs = getCurrentQuestions();
        if (checkedId == -1) {
            currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(null);
        } else if (checkedId == R.id.rbOption1) {
            currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(0);
        } else if (checkedId == R.id.rbOption2) {
            currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(1);
        } else if (checkedId == R.id.rbOption3) {
            currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(2);
        } else if (checkedId == R.id.rbOption4) {
            currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(3);
        }
        paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
    }

    private void startTimer() {
        timer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                Toast.makeText(MockupQuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                finishTest();
            }
        }.start();
        isTimerRunning = true;
        btnPause.setImageResource(R.drawable.ic_pause);

        if (isSubjectTimerRunning && subjectCountDownTimer != null) {
            startSubjectTimer(subjectTimeLeftInMillis);
        }
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isTimerRunning = false;
        btnPause.setImageResource(android.R.drawable.ic_media_play);

        if (subjectCountDownTimer != null) {
            subjectCountDownTimer.cancel();
            isSubjectTimerRunning = false;
        }
    }

    private void resetSubjectTimer() {
        if (subjectCountDownTimer != null) {
            subjectCountDownTimer.cancel();
            subjectCountDownTimer = null;
        }
        isSubjectTimerRunning = false;
        isSubjectTimerFinished = false;
        if (llSubjectTimerContainer != null) {
            llSubjectTimerContainer.setVisibility(View.GONE);
        }
    }

    private void startSubjectTimerIfNeeded() {
        if (subjectCountDownTimer != null || isSubjectTimerFinished) return;

        Integer durationInMins = 0;
        if (supabaseTest.subjects != null) {
            for (SupabaseTest.SubjectModel sm : supabaseTest.subjects) {
                if (sm.subjectName.equals(currentSubject)) {
                    durationInMins = sm.duration;
                    break;
                }
            }
        }

        if (durationInMins != null && durationInMins > 0) {
            isSubjectTimerFinished = false;
            if (llSubjectTimerContainer != null) {
                llSubjectTimerContainer.setVisibility(View.VISIBLE);
            }
            startSubjectTimer(durationInMins * 60 * 1000L);
        } else {
            if (llSubjectTimerContainer != null) {
                llSubjectTimerContainer.setVisibility(View.GONE);
            }
            isSubjectTimerRunning = false;
            isSubjectTimerFinished = true;
        }
        updateSubjectLockStates();
    }

    private void startSubjectTimer(long duration) {
        if (subjectCountDownTimer != null) subjectCountDownTimer.cancel();

        subjectCountDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                subjectTimeLeftInMillis = millisUntilFinished;
                updateSubjectTimerText();
            }

            @Override
            public void onFinish() {
                isSubjectTimerRunning = false;
                isSubjectTimerFinished = true;
                completedSubjects.add(currentSubject);
                tvSubjectTimer.setText("(Finished)");

                int subjectIndex = subjects.indexOf(currentSubject);
                if (subjectIndex < subjects.size() - 1) {
                    saveSelectedOption();
                    currentSubject = subjects.get(subjectIndex + 1);
                    currentQuestionIndexInSubject = 0;
                    resetSubjectTimer();
                    updateSubjectLockStates();
                    updatePalette();
                    showQuestion(0);
                    Toast.makeText(MockupQuizActivity.this, "Time up for " + subjects.get(subjectIndex) + "! Moved to " + currentSubject, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MockupQuizActivity.this, "All subject timers finished!", Toast.LENGTH_SHORT).show();
                    finishTest();
                }
            }
        }.start();
        isSubjectTimerRunning = true;
        updateSubjectLockStates();
    }

    private void updateSubjectTimerText() {
        int minutes = (int) (subjectTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (subjectTimeLeftInMillis / 1000) % 60;
        tvSubjectTimer.setText(String.format(Locale.getDefault(), "(%02d:%02d)", minutes, seconds));
    }

    private void updateTimerText() {
        int hours = (int) (timeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((timeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeString;
        if (hours > 0) {
            timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
        tvTimer.setText(timeString);
    }

    private void finishTest() {
        if (isSubjectTimerRunning) {
            showSubjectLockedDialog(currentSubject);
            return;
        }
        if (timer != null) timer.cancel();
        if (subjectCountDownTimer != null) subjectCountDownTimer.cancel();
        saveSelectedOption();

        int correct = 0;
        int total = 0;
        for (List<Question> list : subjectWiseQuestions.values()) {
            total += list.size();
            for (Question q : list) {
                if (q.getSelectedOptionIndex() != null && q.getSelectedOptionIndex() == q.getCorrectOptionIndex()) {
                    correct++;
                }
            }
        }

        List<Question> allQuestions = new ArrayList<>();
        for (List<Question> list : subjectWiseQuestions.values()) {
            allQuestions.addAll(list);
        }
        Test test = new Test(String.valueOf(supabaseTest.id), supabaseTest.title, supabaseTest.description, supabaseTest.duration, allQuestions);

        Bundle bundle = new Bundle();
        bundle.putInt("correct", correct);
        bundle.putInt("total", total);
        bundle.putSerializable("test", test);

        ResultFragment fragment = new ResultFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (subjectCountDownTimer != null) subjectCountDownTimer.cancel();
    }
}
