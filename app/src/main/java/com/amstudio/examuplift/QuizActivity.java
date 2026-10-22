package com.amstudio.examuplift;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.adapters.PaletteAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.fragments.ResultFragment;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.ReportQuestion;
import com.amstudio.examuplift.models.SupabaseTest;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.utils.QuestionTranslator;
import com.amstudio.examuplift.utils.SavedQuestionsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;

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
    private MaterialButton btnLanguageToggle;
    private ImageView btnPause, btnMenu, btnMarkForReviewStar, btnReport, btnBookmark;
    private PaletteAdapter paletteAdapter;
    private String selectedLanguage = "en";
    
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

        if (questions != null) {
            QuestionTranslator.pretranslateBatch(questions, 0, 5, "en");
            QuestionTranslator.pretranslateBatch(questions, 0, 5, "hi");
        }

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
        btnReport = findViewById(R.id.btnReport);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnLanguageToggle = findViewById(R.id.btnLanguageToggle);

        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        selectedLanguage = sp.getString("pref_question_language", "hi");
        updateLanguageButtonUI();

        if (btnLanguageToggle != null) {
            btnLanguageToggle.setOnClickListener(v -> showLanguageSelectionDialog());
        }

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> showReportDialog());
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> showExitConfirmationDialog());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        if (btnBookmark != null) {
            btnBookmark.setOnClickListener(v -> {
                if (questions != null && currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
                    Question q = questions.get(currentQuestionIndex);
                    boolean saved = SavedQuestionsManager.toggleSaveQuestion(QuizActivity.this, q);
                    Toast.makeText(QuizActivity.this, saved ? "Question saved!" : "Question removed from saved", Toast.LENGTH_SHORT).show();
                    updateInfoStripIcons();
                }
            });
        }
        
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

    private void showLanguageSelectionDialog() {
        String[] languages = new String[]{"Hindi (हिंदी)", "English"};
        int checkedItem = "hi".equalsIgnoreCase(selectedLanguage) ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language / भाषा चुनें")
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String newLang = (which == 0) ? "hi" : "en";
                    if (!newLang.equalsIgnoreCase(selectedLanguage)) {
                        selectedLanguage = newLang;
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("pref_question_language", selectedLanguage)
                                .apply();

                        updateLanguageButtonUI();
                        showQuestion(currentQuestionIndex);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void updateLanguageButtonUI() {
        if (btnLanguageToggle != null) {
            if ("hi".equalsIgnoreCase(selectedLanguage)) {
                btnLanguageToggle.setText("हिंदी");
            } else {
                btnLanguageToggle.setText("English");
            }
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

        // Pre-translate next batch of 5 questions starting from current index in background
        QuestionTranslator.pretranslateBatch(questions, index, 5, "en");
        QuestionTranslator.pretranslateBatch(questions, index, 5, "hi");

        String qTextLang = q.getQuestionTextForLanguage(selectedLanguage);

        boolean hasTranslation = false;
        if ("hi".equalsIgnoreCase(selectedLanguage)) {
            hasTranslation = q.getQuestionTextHindi() != null && !q.getQuestionTextHindi().trim().isEmpty()
                    && q.getOptionsHindi() != null && !q.getOptionsHindi().isEmpty();
        } else if ("en".equalsIgnoreCase(selectedLanguage)) {
            hasTranslation = q.getQuestionTextEnglish() != null && !q.getQuestionTextEnglish().trim().isEmpty()
                    && q.getOptionsEnglish() != null && !q.getOptionsEnglish().isEmpty();
        } else {
            hasTranslation = true;
        }

        if (hasTranslation || (qTextLang != null && !qTextLang.trim().isEmpty() && !qTextLang.equals(q.getQuestionText()))) {
            tvQuestionText.setText(qTextLang);
            bindOptions(q);
        } else {
            tvQuestionText.setText(q.getQuestionText() != null ? q.getQuestionText() : "");
            bindOptions(q);
            QuestionTranslator.translateQuestion(q, selectedLanguage, new QuestionTranslator.OnTranslationCompleteListener() {
                @Override
                public void onSuccess(String translatedQuestionText, List<String> translatedOptions) {
                    if (questions != null && currentQuestionIndex == index) {
                        tvQuestionText.setText(translatedQuestionText);
                        bindOptions(q);
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Fallback
                }
            });
        }
        
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
        if (btnBookmark != null) {
            boolean saved = SavedQuestionsManager.isQuestionSaved(this, q);
            btnBookmark.setColorFilter(saved ? 0xFF22C7B8 : 0xFF757575);
        }
    }

    private void saveSelectedOption() {
        if (paletteAdapter != null) {
            paletteAdapter.notifyItemChanged(currentQuestionIndex);
        }
    }

    private void bindOptions(Question q) {
        List<String> opts = q.getOptionsForLanguage(selectedLanguage);

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

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Exit Test?")
                .setMessage("Are you sure you want to exit the test? Your test progress will be lost unless you submit.")
                .setPositiveButton("Submit Test", (dialog, which) -> {
                    dialog.dismiss();
                    finishTest();
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNeutralButton("Resume Test", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
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

    private void showReportDialog() {
        if (questions == null || currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            Toast.makeText(this, "No question selected", Toast.LENGTH_SHORT).show();
            return;
        }
        Question q = questions.get(currentQuestionIndex);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_question, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupReasons);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelReport);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitReport);

        if (chipGroup != null && etReason != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int id = checkedIds.get(0);
                    Chip chip = dialogView.findViewById(id);
                    if (chip != null && etReason.getText() != null) {
                        etReason.setText(chip.getText().toString());
                        etReason.setSelection(etReason.getText().length());
                    }
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String reason = etReason != null && etReason.getText() != null ? etReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    Toast.makeText(QuizActivity.this, "Please enter or select a reason for reporting", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                submitQuestionReport(q, reason);
            });
        }

        dialog.show();
    }

    private void submitQuestionReport(Question q, String reason) {
        String testTitle = (test != null && test.getTitle() != null) ? test.getTitle() : "Quiz Test";
        String questionText = q.getQuestionText() != null ? q.getQuestionText() : "";
        List<String> options = q.getOptions();
        String correctOptStr = "Option " + (q.getCorrectOptionIndex() + 1);
        if (options != null && q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < options.size()) {
            correctOptStr = options.get(q.getCorrectOptionIndex());
        }

        ReportQuestion report = new ReportQuestion(
                q.getId(),
                questionText,
                options,
                correctOptStr,
                reason,
                testTitle
        );

        Toast.makeText(this, "Submitting report...", Toast.LENGTH_SHORT).show();

        ApiClient.getInterface().reportQuestion(report).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(QuizActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(QuizActivity.this, "Failed to submit report. Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(QuizActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}