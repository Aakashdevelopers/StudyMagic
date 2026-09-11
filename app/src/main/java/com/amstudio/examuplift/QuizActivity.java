package com.amstudio.examuplift;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.adapters.PaletteAdapter;
import com.amstudio.examuplift.fragments.ResultFragment;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.SupabaseTest;
import com.amstudio.examuplift.models.Test;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
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
    private LinearProgressIndicator quizProgress;
    private RadioGroup rgOptions;
    private RadioButton rb1, rb2, rb3, rb4;
    private Button btnSaveNext, btnMarkNext, btnClear, btnSubmitTop, btnPrevious;
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

        if (getIntent() != null) {
            if (getIntent().hasExtra("supabaseTest")) {
                SupabaseTest sTest =
                        (SupabaseTest) getIntent().getSerializableExtra("supabaseTest");
                if (sTest != null) test = sTest.toTest();
            } else if (getIntent().hasExtra("test")) {
                test = (Test) getIntent().getSerializableExtra("test");
            }
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
        btnPrevious = findViewById(R.id.btnPrevious);
        
        btnPause = findViewById(R.id.btnPause);
        btnMenu = findViewById(R.id.btnMenu);
        btnMarkForReviewStar = findViewById(R.id.btnMarkForReviewStar);
        
        tvQuizTitle.setText(test.getTitle());

        paletteAdapter = new PaletteAdapter(questions, index -> {
            if (questions != null && currentQuestionIndex < questions.size()) {
                questions.get(currentQuestionIndex).setVisited(true);
            }
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
            if (questions != null && currentQuestionIndex < questions.size()) {
                questions.get(currentQuestionIndex).setSelectedOptionIndex(null);
                updateOptionUI();
                if (paletteAdapter != null) {
                    paletteAdapter.notifyItemChanged(currentQuestionIndex);
                }
            }
        });

        btnMarkNext.setOnClickListener(view -> {
            if (questions != null && currentQuestionIndex < questions.size()) {
                Question q = questions.get(currentQuestionIndex);
                q.setVisited(true);
                q.setMarkedForReview(true);
            }
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSaveNext.setOnClickListener(view -> {
            if (questions != null && currentQuestionIndex < questions.size()) {
                Question q = questions.get(currentQuestionIndex);
                q.setVisited(true);
                q.setMarkedForReview(false);
            }
            saveSelectedOption();
            goToNextQuestion();
        });

        btnPrevious.setOnClickListener(view -> {
            if (questions != null && currentQuestionIndex < questions.size()) {
                questions.get(currentQuestionIndex).setVisited(true);
            }
            goToPreviousQuestion();
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
        if (paletteAdapter == null) {
            paletteAdapter = new PaletteAdapter(questions, index -> {
                if (questions != null && currentQuestionIndex < questions.size()) {
                    questions.get(currentQuestionIndex).setVisited(true);
                }
                currentQuestionIndex = index;
                showQuestion(index);
            });
        } else {
            paletteAdapter.notifyDataSetChanged();
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_quiz_palette, null);
        
        RecyclerView rvPalette = view.findViewById(R.id.rvQuestionPalette);
        rvPalette.setLayoutManager(new GridLayoutManager(this, 5));
        rvPalette.setAdapter(paletteAdapter);
        
        paletteAdapter.setOnItemClickListener(index -> {
            if (questions != null && currentQuestionIndex < questions.size()) {
                questions.get(currentQuestionIndex).setVisited(true);
            }
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

    private void goToPreviousQuestion() {
        saveSelectedOption();
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            showQuestion(currentQuestionIndex);
        } else {
            Toast.makeText(this, "This is the first question", Toast.LENGTH_SHORT).show();
        }
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
        if (questions == null || index < 0 || index >= questions.size()) return;
        Question q = questions.get(index);
        tvQuestionNoPill.setText(String.valueOf(index + 1));
        tvQuestionText.setText(q.getQuestionText() != null ? q.getQuestionText() : "");
        
        updateProgress();
        updateInfoStripIcons();
        
        if (q.getImageUrl() != null && !q.getImageUrl().trim().isEmpty()) {
            String imgUrl = q.getImageUrl().trim().replace(" ", "%20");
            ivQuestionImage.setVisibility(View.VISIBLE);
            Picasso.get().load(imgUrl).into(ivQuestionImage, new Callback() {
                @Override
                public void onSuccess() {
                    if (ivQuestionImage != null) {
                        ivQuestionImage.post(() -> ivQuestionImage.requestLayout());
                    }
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    if (ivQuestionImage != null) {
                        ivQuestionImage.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        bindOptions(q);

        updateOptionUI();
        
        if (paletteAdapter != null) {
            paletteAdapter.notifyDataSetChanged(); 
        }
        
        if (btnPrevious != null) {
            boolean canGoPrev = currentQuestionIndex > 0;
            btnPrevious.setEnabled(canGoPrev);
            btnPrevious.setAlpha(canGoPrev ? 1.0f : 0.5f);
        }

        if (index == questions.size() - 1) {
            btnSaveNext.setText("FINISH");
        } else {
            btnSaveNext.setText("Save & Next");
        }
    }

    private void updateProgress() {
        if (questions == null || questions.isEmpty()) return;
        int attemptedCount = 0;
        for (Question q : questions) {
            if (q.getSelectedOptionIndex() != null) attemptedCount++;
        }
        int progress = (int) (((float) attemptedCount / questions.size()) * 100);
        quizProgress.setProgress(progress);
    }

    private void updateInfoStripIcons() {
        if (questions == null || currentQuestionIndex >= questions.size()) return;
        Question q = questions.get(currentQuestionIndex);
        btnMarkForReviewStar.setColorFilter(q.isMarkedForReview() ? 0xFFFF1744 : 0xFF757575);
    }

    private void saveSelectedOption() {
        if (paletteAdapter != null) {
            paletteAdapter.notifyItemChanged(currentQuestionIndex);
        }
    }

    private void bindOptions(Question q) {
        List<String> opts = q.getOptions();

        bindOption(llOpt1, tvOptText1, ivOpt1, rb1, (opts != null && opts.size() > 0) ? opts.get(0) : null);
        bindOption(llOpt2, tvOptText2, ivOpt2, rb2, (opts != null && opts.size() > 1) ? opts.get(1) : null);
        bindOption(llOpt3, tvOptText3, ivOpt3, rb3, (opts != null && opts.size() > 2) ? opts.get(2) : null);
        bindOption(llOpt4, tvOptText4, ivOpt4, rb4, (opts != null && opts.size() > 3) ? opts.get(3) : null);
    }

    private boolean isImageUrl(String text) {
        if (text == null) return false;
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true;
        }
        return trimmed.contains("i.ibb.co/") || trimmed.contains(".png") || trimmed.contains(".jpg") || trimmed.contains(".jpeg") || trimmed.contains(".webp");
    }

    private void bindOption(LinearLayout container, TextView tvText, ImageView iv, RadioButton rb, String optionValue) {
        if (optionValue == null || optionValue.trim().isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        String trimmed = optionValue.trim();

        if (isImageUrl(trimmed)) {
            if (tvText != null) tvText.setVisibility(View.GONE);
            if (rb != null) rb.setText("");
            iv.setVisibility(View.VISIBLE);

            String url = trimmed.replace(" ", "%20");
            Picasso.get().load(url).into(iv, new Callback() {
                @Override
                public void onSuccess() {
                    iv.post(() -> {
                        iv.requestLayout();
                        if (container != null) container.requestLayout();
                    });
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    if (tvText != null) {
                        tvText.setVisibility(View.VISIBLE);
                        tvText.setText(optionValue);
                    }
                    iv.setVisibility(View.GONE);
                }
            });
        } else {
            if (tvText != null) {
                tvText.setVisibility(View.VISIBLE);
                tvText.setText(optionValue);
                if (rb != null) rb.setText("");
            } else if (rb != null) {
                rb.setText(optionValue);
            }
            iv.setVisibility(View.GONE);
        }
    }

    private void setupOptionClick(LinearLayout layout, RadioButton rb, int optionIndex) {
        View.OnClickListener listener = v -> selectOption(optionIndex);
        layout.setOnClickListener(listener);
        rb.setOnClickListener(listener);
    }

    private void selectOption(int optionIndex) {
        if (questions == null || currentQuestionIndex >= questions.size()) return;
        Question q = questions.get(currentQuestionIndex);
        q.setSelectedOptionIndex(optionIndex);
        updateOptionUI();
        if (paletteAdapter != null) {
            paletteAdapter.notifyItemChanged(currentQuestionIndex);
        }
    }

    private void updateOptionUI() {
        if (questions == null || currentQuestionIndex >= questions.size()) return;
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
        bundle.putBoolean("isMockTest", false);

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