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
import com.squareup.picasso.Picasso;
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private Test test;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    
    private TextView tvTimer, tvQuizTitle, tvQuestionNoPill, tvQuestionText;
    private TextView tvOptText1, tvOptText2, tvOptText3, tvOptText4;
    private ImageView ivQuestionImage, ivOpt1, ivOpt2, ivOpt3, ivOpt4;
    private LinearLayout llOpt1, llOpt2, llOpt3, llOpt4;
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
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        tvOptText1 = findViewById(R.id.tvOptText1);
        tvOptText2 = findViewById(R.id.tvOptText2);
        tvOptText3 = findViewById(R.id.tvOptText3);
        tvOptText4 = findViewById(R.id.tvOptText4);
        ivOpt1 = findViewById(R.id.ivOption1);
        ivOpt2 = findViewById(R.id.ivOption2);
        ivOpt3 = findViewById(R.id.ivOption3);
        ivOpt4 = findViewById(R.id.ivOption4);
        llOpt1 = findViewById(R.id.llOption1);
        llOpt2 = findViewById(R.id.llOption2);
        llOpt3 = findViewById(R.id.llOption3);
        llOpt4 = findViewById(R.id.llOption4);

        setupOptionClick(llOpt1, rb1, 0);
        setupOptionClick(llOpt2, rb2, 1);
        setupOptionClick(llOpt3, rb3, 2);
        setupOptionClick(llOpt4, rb4, 3);
        
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
        
        if (q.getImageUrl() != null && !q.getImageUrl().trim().isEmpty()) {
            ivQuestionImage.setVisibility(View.VISIBLE);
            Picasso.get().load(q.getImageUrl()).into(ivQuestionImage, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {}
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        bindOption(tvOptText1, ivOpt1, rb1, q.getOptions().get(0));
        bindOption(tvOptText2, ivOpt2, rb2, q.getOptions().get(1));
        bindOption(tvOptText3, ivOpt3, rb3, q.getOptions().get(2));
        bindOption(tvOptText4, ivOpt4, rb4, q.getOptions().get(3));

        updateOptionUI();
        
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
        paletteAdapter.notifyItemChanged(currentQuestionIndex);
    }

    private void bindOption(TextView tvText, ImageView iv, RadioButton rb, String optionValue) {
        if (optionValue != null) {
            String trimmed = optionValue.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                tvText.setVisibility(View.GONE);
                iv.setVisibility(View.VISIBLE);
                Picasso.get().load(trimmed).into(iv, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
        }
        tvText.setVisibility(View.VISIBLE);
        tvText.setText(optionValue);
        iv.setVisibility(View.GONE);
    }

    private void setupOptionClick(LinearLayout layout, RadioButton rb, int optionIndex) {
        View.OnClickListener listener = v -> selectOption(optionIndex);
        layout.setOnClickListener(listener);
        rb.setOnClickListener(listener);
    }

    private void selectOption(int optionIndex) {
        Question q = questions.get(currentQuestionIndex);
        q.setSelectedOptionIndex(optionIndex);
        updateOptionUI();
        paletteAdapter.notifyItemChanged(currentQuestionIndex);
    }

    private void updateOptionUI() {
        Question q = questions.get(currentQuestionIndex);
        Integer sel = q.getSelectedOptionIndex();

        setOptionSelected(llOpt1, rb1, sel != null && sel == 0);
        setOptionSelected(llOpt2, rb2, sel != null && sel == 1);
        setOptionSelected(llOpt3, rb3, sel != null && sel == 2);
        setOptionSelected(llOpt4, rb4, sel != null && sel == 3);
    }

    private void setOptionSelected(LinearLayout layout, RadioButton rb, boolean isSelected) {
        rb.setChecked(isSelected);
        layout.setSelected(isSelected);
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