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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.adapters.PaletteAdapter;
import com.amstudio.studymagic.fragments.ResultFragment;
import com.amstudio.studymagic.models.Question;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private Test test;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    
    private TextView tvTimer, tvQuizTitle, tvQuestionNoPill, tvQuestionText;
    private com.google.android.material.progressindicator.LinearProgressIndicator quizProgress;
    private RadioGroup rgOptions;
    private RadioButton rb1, rb2, rb3, rb4;
    private Button btnSaveNext, btnMarkNext, btnClear, btnSubmitTop;
    private ImageView btnPause, btnMenu, btnMarkForReviewStar;
    private PaletteAdapter paletteAdapter;
    
    private CountDownTimer timer;
    private long timeLeftInMillis;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        if (getIntent() != null && getIntent().hasExtra("test")) {
            test = (Test) getIntent().getSerializableExtra("test");
            if (test != null) questions = test.getQuestions();
        }

        if (questions == null || questions.isEmpty()) {
            finish();
            return;
        }

        initViews();
        timeLeftInMillis = test.getDurationMinutes() * 60 * 1000L;
        startTimer();
        showQuestion(currentQuestionIndex);
    }

    private void initViews() {
        View main = findViewById(R.id.main);
        LinearLayout llTopBar = findViewById(R.id.llTopBar);
        LinearLayout llBottomActions = findViewById(R.id.llBottomActions);

        // Set status bar icons to light since top bar is dark
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

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
        
        tvQuizTitle.setText(test.getTitle());

        paletteAdapter = new PaletteAdapter(questions, index -> {
            currentQuestionIndex = index;
            showQuestion(index);
        });

        btnMenu.setOnClickListener(view -> showPaletteBottomSheet());

        btnPause.setOnClickListener(view -> {
            if (isTimerRunning) pauseTimer();
            else startTimer();
        });

        btnClear.setOnClickListener(view -> {
            rgOptions.clearCheck();
            questions.get(currentQuestionIndex).setSelectedOptionIndex(null);
            paletteAdapter.notifyItemChanged(currentQuestionIndex);
        });

        btnMarkNext.setOnClickListener(view -> {
            questions.get(currentQuestionIndex).setMarkedForReview(true);
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSaveNext.setOnClickListener(view -> {
            questions.get(currentQuestionIndex).setMarkedForReview(false);
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSubmitTop.setOnClickListener(view -> finishTest());

        btnMarkForReviewStar.setOnClickListener(view -> {
            boolean current = questions.get(currentQuestionIndex).isMarkedForReview();
            questions.get(currentQuestionIndex).setMarkedForReview(!current);
            updateInfoStripIcons();
            paletteAdapter.notifyItemChanged(currentQuestionIndex);
        });
    }

    private void showPaletteBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_quiz_palette, null);
        
        RecyclerView rvPalette = view.findViewById(R.id.rvQuestionPalette);
        rvPalette.setAdapter(paletteAdapter);
        
        // Update selection logic for bottom sheet
        paletteAdapter.setOnItemClickListener(index -> {
            currentQuestionIndex = index;
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
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
        } else {
            finishTest();
        }
    }

    private void showQuestion(int index) {
        Question q = questions.get(index);
        tvQuestionNoPill.setText(String.valueOf(index + 1));
        tvQuestionText.setText(q.getQuestionText());
        
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
        
        paletteAdapter.notifyDataSetChanged(); 
        
        // Update button text for last question
        if (index == questions.size() - 1) {
            btnSaveNext.setText("FINISH");
        } else {
            btnSaveNext.setText("Save & Next");
        }
    }

    private void updateProgress() {
        int attemptedCount = 0;
        for (Question q : questions) {
            if (q.getSelectedOptionIndex() != null) attemptedCount++;
        }
        int progress = (int) (((float) attemptedCount / questions.size()) * 100);
        quizProgress.setProgress(progress);
    }

    private void updateInfoStripIcons() {
        Question q = questions.get(currentQuestionIndex);
        btnMarkForReviewStar.setColorFilter(q.isMarkedForReview() ? 0xFFFF1744 : 0xFF757575);
    }

    private void saveSelectedOption() {
        int checkedId = rgOptions.getCheckedRadioButtonId();
        if (checkedId == -1) {
            questions.get(currentQuestionIndex).setSelectedOptionIndex(null);
        } else if (checkedId == R.id.rbOption1) {
            questions.get(currentQuestionIndex).setSelectedOptionIndex(0);
        } else if (checkedId == R.id.rbOption2) {
            questions.get(currentQuestionIndex).setSelectedOptionIndex(1);
        } else if (checkedId == R.id.rbOption3) {
            questions.get(currentQuestionIndex).setSelectedOptionIndex(2);
        } else if (checkedId == R.id.rbOption4) {
            questions.get(currentQuestionIndex).setSelectedOptionIndex(3);
        }
        paletteAdapter.notifyItemChanged(currentQuestionIndex);
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
                Toast.makeText(QuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
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
        for (Question q : questions) {
            if (q.getSelectedOptionIndex() != null && q.getSelectedOptionIndex() == q.getCorrectOptionIndex()) {
                correct++;
            }
        }

        Bundle bundle = new Bundle();
        bundle.putInt("correct", correct);
        bundle.putInt("total", questions.size());
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