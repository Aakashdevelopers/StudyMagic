package com.amstudio.studymagic;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockupQuizActivity extends AppCompatActivity {

    private SupabaseTest supabaseTest;
    private Map<String, List<Question>> subjectWiseQuestions = new LinkedHashMap<>();
    private List<String> subjects = new ArrayList<>();
    private String currentSubject;
    private int currentQuestionIndexInSubject = 0;

    private TextView tvTimer, tvQuizTitle, tvQuestionNoPill, tvQuestionText, tvSubjectLabel;
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
        timeLeftInMillis = supabaseTest.duration * 60 * 1000L;
        startTimer();
        showQuestion(0);
    }

    private void parseQuestions() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(supabaseTest.questionsJson);
        Type mapType = new TypeToken<Map<String, List<Question>>>(){}.getType();
        Map<String, List<Question>> map = gson.fromJson(jsonStr, mapType);
        if (map != null) {
            subjectWiseQuestions.putAll(map);
            subjects.addAll(map.keySet());
        }
    }

    private void initViews() {
        View main = findViewById(R.id.main);
        LinearLayout llTopBar = findViewById(R.id.llTopBar);
        LinearLayout llBottomActions = findViewById(R.id.llBottomActions);

        // Set status bar icons to dark since top bar is white
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        // Apply insets manually to the root view for better reliability
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

        subjectAdapter = new SubjectTabAdapter(subjects, subject -> {
            currentSubject = subject;
            currentQuestionIndexInSubject = 0;
            updatePalette();
            showQuestion(0);
        });
        rvSubjects.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSubjects.setAdapter(subjectAdapter);

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
            // Check if there is a next subject
            int subjectIndex = subjects.indexOf(currentSubject);
            if (subjectIndex < subjects.size() - 1) {
                currentSubject = subjects.get(subjectIndex + 1);
                currentQuestionIndexInSubject = 0;
                subjectAdapter.setSelectedSubject(currentSubject);
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
        tvQuestionNoPill.setText(String.valueOf(index + 1));
        tvQuestionText.setText(q.getQuestionText());
        tvSubjectLabel.setText("Subject: " + currentSubject);
        
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
        
        // Update button text
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
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isTimerRunning = false;
        btnPause.setImageResource(android.R.drawable.ic_media_play);
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
        if (timer != null) timer.cancel();
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

        // To reuse ResultFragment, we need a Test object
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
    }
}
