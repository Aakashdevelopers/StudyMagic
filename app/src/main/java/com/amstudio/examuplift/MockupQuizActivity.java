package com.amstudio.examuplift;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.adapters.PaletteAdapter;
import com.amstudio.examuplift.adapters.SubjectTabAdapter;
import com.amstudio.examuplift.fragments.ResultFragment;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.SupabaseTest;
import com.amstudio.examuplift.utils.SavedQuestionsManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.amstudio.examuplift.models.Test;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.models.ReportQuestion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import android.content.SharedPreferences;
import com.google.android.material.button.MaterialButton;
import com.amstudio.examuplift.utils.QuestionTranslator;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

public class MockupQuizActivity extends AppCompatActivity {

    private SupabaseTest supabaseTest;
    private Map<String, List<Question>> subjectWiseQuestions = new LinkedHashMap<>();
    private List<String> subjects = new ArrayList<>();
    private String currentSubject;
    private int currentQuestionIndexInSubject = 0;
    private String selectedLanguage = "en";

    private Set<String> lockedSubjects = new HashSet<>();
    private Set<String> completedSubjects = new HashSet<>();

    private TextView tvTimer, tvQuizTitle, tvQuestionNoPill, tvQuestionText, tvSubjectLabel, tvSubjectTimer;
    private TextView tvOptText1, tvOptText2, tvOptText3, tvOptText4;
    private ImageView ivQuestionImage, ivOpt1, ivOpt2, ivOpt3, ivOpt4;
    private LinearLayout llOpt1, llOpt2, llOpt3, llOpt4;
    private LinearLayout llSubjectTimerContainer;
    private MaterialButton btnLanguageToggle;
    private LinearProgressIndicator quizProgress;
    private RadioGroup rgOptions;
    private RadioButton rb1, rb2, rb3, rb4;
    private Button btnSaveNext, btnMarkNext, btnClear, btnSubmitTop, btnPrevious;
    private ImageView btnPause, btnMenu, btnMarkForReviewStar, btnReport, btnBookmark;
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

        if (supabaseTest == null || subjects.isEmpty()) {
            Toast.makeText(this, "No questions found for this test", Toast.LENGTH_SHORT).show();
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

        timeLeftInMillis = supabaseTest.duration > 0 ? supabaseTest.duration * 60 * 1000L : 30 * 60 * 1000L;
        startTimer();

        // Immediately trigger background pre-translation for starting 5 questions
        List<Question> initialQs = getCurrentQuestions();
        if (initialQs != null) {
            QuestionTranslator.pretranslateBatch(initialQs, 0, 5, "en");
            QuestionTranslator.pretranslateBatch(initialQs, 0, 5, "hi");
        }

        showQuestion(0);
    }

    private void parseQuestions() {
        if (supabaseTest == null) return;
        Gson gson = new Gson();
        subjectWiseQuestions.clear();
        subjects.clear();

        boolean parsedFromQuestionsJson = false;

        if (supabaseTest.questionsJson != null) {
            try {
                String jsonStr = (supabaseTest.questionsJson instanceof String) ?
                        (String) supabaseTest.questionsJson : gson.toJson(supabaseTest.questionsJson);

                if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                    String trimmed = jsonStr.trim();
                    if (trimmed.startsWith("{")) {
                        Map<String, Object> map = gson.fromJson(trimmed, new TypeToken<Map<String, Object>>() {}.getType());

                        if (map != null && map.containsKey("subjects")) {
                            Type subjectListType = new TypeToken<List<SupabaseTest.SubjectModel>>() {}.getType();
                            List<SupabaseTest.SubjectModel> parsedSubjects = gson.fromJson(gson.toJson(map.get("subjects")), subjectListType);
                            if (parsedSubjects != null && !parsedSubjects.isEmpty()) {
                                supabaseTest.subjects = parsedSubjects;
                            }

                            if (map.containsKey("test_type") && map.get("test_type") != null) {
                                supabaseTest.testType = String.valueOf(map.get("test_type"));
                            }
                            if (map.containsKey("is_subject_timer_enabled") && map.get("is_subject_timer_enabled") != null) {
                                try {
                                    supabaseTest.isSubjectTimerEnabled = Boolean.parseBoolean(String.valueOf(map.get("is_subject_timer_enabled")));
                                } catch (Exception ignored) {}
                            }
                        } else if (map != null && map.containsKey("questions")) {
                            Type listType = new TypeToken<List<Question>>() {}.getType();
                            List<Question> qs = gson.fromJson(gson.toJson(map.get("questions")), listType);
                            if (qs != null && !qs.isEmpty()) {
                                subjectWiseQuestions.put("General", qs);
                                subjects.add("General");
                                parsedFromQuestionsJson = true;
                            }
                        } else if (map != null) {
                            for (Map.Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                if ("test_type".equalsIgnoreCase(key) || "is_subject_timer_enabled".equalsIgnoreCase(key)) continue;
                                Type listType = new TypeToken<List<Question>>() {}.getType();
                                List<Question> qs = gson.fromJson(gson.toJson(entry.getValue()), listType);
                                if (qs != null && !qs.isEmpty()) {
                                    subjectWiseQuestions.put(key, qs);
                                    subjects.add(key);
                                    parsedFromQuestionsJson = true;
                                }
                            }
                        }
                    } else if (trimmed.startsWith("[")) {
                        Type listType = new TypeToken<List<Question>>() {}.getType();
                        List<Question> qs = gson.fromJson(trimmed, listType);
                        if (qs != null && !qs.isEmpty()) {
                            subjectWiseQuestions.put("General", qs);
                            subjects.add("General");
                            parsedFromQuestionsJson = true;
                        }
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

        if (!parsedFromQuestionsJson && supabaseTest.subjects != null) {
            for (SupabaseTest.SubjectModel sm : supabaseTest.subjects) {
                if (sm.questionsJson == null) continue;
                String jsonStr = gson.toJson(sm.questionsJson);
                Type listType = new TypeToken<List<Question>>() {}.getType();
                List<Question> qs = gson.fromJson(jsonStr, listType);
                if (qs != null && !qs.isEmpty()) {
                    String subName = sm.subjectName != null ? sm.subjectName : "Subject";
                    subjectWiseQuestions.put(subName, qs);
                    subjects.add(subName);
                }
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
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        tvSubjectLabel = findViewById(R.id.tvSubjectLabel);
        tvSubjectTimer = findViewById(R.id.tvSubjectTimer);
        llSubjectTimerContainer = findViewById(R.id.llSubjectTimerContainer);
        quizProgress = findViewById(R.id.quizProgress);
        rgOptions = findViewById(R.id.rgOptions);
        rb1 = findViewById(R.id.rbOption1);
        rb2 = findViewById(R.id.rbOption2);
        rb3 = findViewById(R.id.rbOption3);
        rb4 = findViewById(R.id.rbOption4);
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
        rvSubjects = findViewById(R.id.rvSubjects);

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
                List<Question> currentQs = getCurrentQuestions();
                if (currentQs != null && currentQuestionIndexInSubject >= 0 && currentQuestionIndexInSubject < currentQs.size()) {
                    Question q = currentQs.get(currentQuestionIndexInSubject);
                    boolean saved = SavedQuestionsManager.toggleSaveQuestion(MockupQuizActivity.this, q);
                    Toast.makeText(MockupQuizActivity.this, saved ? "Question saved!" : "Question removed from saved", Toast.LENGTH_SHORT).show();
                    updateInfoStripIcons();
                }
            });
        }

        tvQuizTitle.setText(supabaseTest.title);

        subjectAdapter = new SubjectTabAdapter(subjects, (subject, isLocked) -> {
            if (isLocked) {
                showSubjectLockedDialog(subject);
                return;
            }
            if (!subject.equals(currentSubject)) {
                List<Question> currentQs = getCurrentQuestions();
                if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                    currentQs.get(currentQuestionIndexInSubject).setVisited(true);
                }
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
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                currentQs.get(currentQuestionIndexInSubject).setSelectedOptionIndex(null);
                updateOptionUI();
                if (paletteAdapter != null) {
                    paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
                }
            }
        });

        btnMarkNext.setOnClickListener(view -> {
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                Question q = currentQs.get(currentQuestionIndexInSubject);
                q.setVisited(true);
                q.setMarkedForReview(true);
            }
            saveSelectedOption();
            goToNextQuestion();
        });

        btnSaveNext.setOnClickListener(view -> {
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                Question q = currentQs.get(currentQuestionIndexInSubject);
                q.setVisited(true);
                q.setMarkedForReview(false);
            }
            saveSelectedOption();
            goToNextQuestion();
        });

        btnPrevious.setOnClickListener(view -> {
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                currentQs.get(currentQuestionIndexInSubject).setVisited(true);
            }
            goToPreviousQuestion();
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
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                currentQs.get(currentQuestionIndexInSubject).setVisited(true);
            }
            currentQuestionIndexInSubject = index;
            showQuestion(index);
        });
    }

    private List<Question> getCurrentQuestions() {
        return subjectWiseQuestions.get(currentSubject);
    }

    private void showPaletteBottomSheet() {
        if (paletteAdapter == null) {
            updatePalette();
        } else {
            paletteAdapter.notifyDataSetChanged();
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_quiz_palette, null);

        RecyclerView rvPalette = view.findViewById(R.id.rvQuestionPalette);
        rvPalette.setLayoutManager(new GridLayoutManager(this, 5));
        rvPalette.setAdapter(paletteAdapter);

        paletteAdapter.setOnItemClickListener(index -> {
            List<Question> currentQs = getCurrentQuestions();
            if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
                currentQs.get(currentQuestionIndexInSubject).setVisited(true);
            }
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

    private void goToPreviousQuestion() {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
            currentQs.get(currentQuestionIndexInSubject).setVisited(true);
        }
        saveSelectedOption();
        if (currentQuestionIndexInSubject > 0) {
            currentQuestionIndexInSubject--;
            showQuestion(currentQuestionIndexInSubject);
        } else {
            int subjectIndex = subjects.indexOf(currentSubject);
            if (subjectIndex > 0) {
                String prevSubject = subjects.get(subjectIndex - 1);
                if (isSubjectTimerRunning || completedSubjects.contains(prevSubject)) {
                    Toast.makeText(this, "Previous section (" + prevSubject + ") is locked", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentSubject = prevSubject;
                List<Question> prevQs = getCurrentQuestions();
                currentQuestionIndexInSubject = (prevQs != null && !prevQs.isEmpty()) ? prevQs.size() - 1 : 0;
                resetSubjectTimer();
                updateSubjectLockStates();
                updatePalette();
                showQuestion(currentQuestionIndexInSubject);
                Toast.makeText(this, "Previous Subject: " + currentSubject, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This is the first question", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goToNextQuestion() {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs != null && currentQuestionIndexInSubject < currentQs.size()) {
            currentQs.get(currentQuestionIndexInSubject).setVisited(true);
        }
        if (currentQs != null && currentQuestionIndexInSubject < currentQs.size() - 1) {
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
                        showQuestion(currentQuestionIndexInSubject);
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

    private void showQuestion(int index) {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs == null || index < 0 || index >= currentQs.size()) return;
        Question q = currentQs.get(index);
        tvQuestionNoPill.setText("Q. " + (index + 1));

        // Pre-translate next batch of 5 questions starting from current index in background
        QuestionTranslator.pretranslateBatch(currentQs, index, 5, "en");
        QuestionTranslator.pretranslateBatch(currentQs, index, 5, "hi");

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
                    List<Question> qs = getCurrentQuestions();
                    if (qs != null && currentQuestionIndexInSubject == index) {
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
        tvSubjectLabel.setText("Subject: " + (currentSubject != null ? currentSubject : ""));

        if (index == 0 && supabaseTest != null && supabaseTest.isSubjectTimerEnabled) {
            startSubjectTimerIfNeeded();
        }

        updateSubjectLockStates();
        updateProgress();
        updateInfoStripIcons();

        updateOptionUI();

        if (paletteAdapter != null) {
            paletteAdapter.notifyDataSetChanged();
        }

        if (btnPrevious != null) {
            boolean canGoPrev;
            if (isSubjectTimerRunning) {
                canGoPrev = currentQuestionIndexInSubject > 0;
            } else {
                canGoPrev = currentQuestionIndexInSubject > 0 || subjects.indexOf(currentSubject) > 0;
            }
            btnPrevious.setEnabled(canGoPrev);
            btnPrevious.setAlpha(canGoPrev ? 1.0f : 0.5f);
        }

        if (index == currentQs.size() - 1 && subjects.indexOf(currentSubject) == subjects.size() - 1) {
            btnSaveNext.setText("FINISH");
        } else {
            btnSaveNext.setText("Save & Next");
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
                    if (iv != null) {
                        iv.post(() -> {
                            iv.requestLayout();
                            if (container != null) container.requestLayout();
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    if (tvText != null) {
                        tvText.setVisibility(View.VISIBLE);
                        tvText.setText(optionValue);
                    }
                    if (iv != null) iv.setVisibility(View.GONE);
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
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs == null || currentQuestionIndexInSubject >= currentQs.size()) return;
        Question q = currentQs.get(currentQuestionIndexInSubject);
        q.setSelectedOptionIndex(optionIndex);
        updateOptionUI();
        if (paletteAdapter != null) {
            paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
        }
    }

    private void updateOptionUI() {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs == null || currentQuestionIndexInSubject >= currentQs.size()) return;
        Question q = currentQs.get(currentQuestionIndexInSubject);
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

    private void updateProgress() {
        int totalQuestions = 0;
        int attemptedCount = 0;
        for (List<Question> list : subjectWiseQuestions.values()) {
            if (list == null) continue;
            totalQuestions += list.size();
            for (Question q : list) {
                if (q.getSelectedOptionIndex() != null) attemptedCount++;
            }
        }
        if (totalQuestions == 0) return;
        int progress = (int) (((float) attemptedCount / totalQuestions) * 100);
        quizProgress.setProgress(progress);
    }

    private void updateInfoStripIcons() {
        List<Question> currentQs = getCurrentQuestions();
        if (currentQs == null || currentQuestionIndexInSubject >= currentQs.size()) return;
        Question q = currentQs.get(currentQuestionIndexInSubject);
        btnMarkForReviewStar.setColorFilter(q.isMarkedForReview() ? 0xFFFF1744 : 0xFF757575);
        if (btnBookmark != null) {
            boolean saved = SavedQuestionsManager.isQuestionSaved(this, q);
            btnBookmark.setColorFilter(saved ? 0xFF22C7B8 : 0xFF757575);
        }
    }

    private void saveSelectedOption() {
        if (paletteAdapter != null) {
            paletteAdapter.notifyItemChanged(currentQuestionIndexInSubject);
        }
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
        Test test = new Test(String.valueOf(supabaseTest.id), supabaseTest.title, supabaseTest.description, supabaseTest.duration, allQuestions, supabaseTest != null ? supabaseTest.negativeMarking : null, supabaseTest != null ? supabaseTest.correctMarks : null, supabaseTest != null ? supabaseTest.totalMarks : null);

        Bundle bundle = new Bundle();
        bundle.putInt("correct", correct);
        bundle.putInt("total", total);
        bundle.putSerializable("test", test);
        bundle.putBoolean("isMockTest", true);

        ResultFragment fragment = new ResultFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    private void showReportDialog() {
        List<Question> currentQs = getCurrentQuestions();
        Question q = (currentQs != null && currentQuestionIndexInSubject >= 0 && currentQuestionIndexInSubject < currentQs.size()) ? currentQs.get(currentQuestionIndexInSubject) : null;
        if (q == null) {
            Toast.makeText(this, "No question selected", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(MockupQuizActivity.this, "Please enter or select a reason for reporting", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                submitQuestionReport(q, reason);
            });
        }

        dialog.show();
    }

    private void submitQuestionReport(Question q, String reason) {
        String testTitle = (supabaseTest != null && supabaseTest.title != null) ? supabaseTest.title : "Mockup Test";
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
                    Toast.makeText(MockupQuizActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MockupQuizActivity.this, "Failed to submit report. Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MockupQuizActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (subjectCountDownTimer != null) subjectCountDownTimer.cancel();
    }
}