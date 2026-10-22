package com.amstudio.examuplift;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.adapters.ChatAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.api.PollinationsApi;
import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.models.ai.ChatMessage;
import com.amstudio.examuplift.models.ai.ChatRequest;
import com.amstudio.examuplift.models.ai.ChatResponse;
import com.amstudio.examuplift.models.ai.RapidApiRequest;
import com.amstudio.examuplift.models.ai.RapidApiResponse;
import com.amstudio.examuplift.utils.Constants;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.chip.Chip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatActivity extends AppCompatActivity {

    public static final String EXTRA_TEST = "extra_test";
    public static final String EXTRA_CORRECT = "extra_correct";
    public static final String EXTRA_TOTAL = "extra_total";

    private Test currentTest;
    private int correctCount = 0;
    private int totalCount = 0;

    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private EditText etChatMessage;
    private View btnSendChat;
    private View llChatLoading;

    // Chat API history context
    private List<ChatRequest.Message> apiHistoryMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        View rootView = findViewById(R.id.rootViewChat);
        View llHeader = findViewById(R.id.llHeader);

        WindowInsetsUtil.applyTopInset(llHeader);
        WindowInsetsUtil.setLightStatusBar(this, false);
        WindowInsetsUtil.handleImeInsets(rootView, rvChatMessages);

        Intent intent = getIntent();
        if (intent != null) {
            currentTest = (Test) intent.getSerializableExtra(EXTRA_TEST);
            correctCount = intent.getIntExtra(EXTRA_CORRECT, 0);
            totalCount = intent.getIntExtra(EXTRA_TOTAL, 0);
        }

        chatMessages = new ArrayList<>();
        apiHistoryMessages = new ArrayList<>();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnClearChat = findViewById(R.id.btnClearChat);
        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> {
                chatMessages.clear();
                apiHistoryMessages.clear();
                chatAdapter.notifyDataSetChanged();
                initSystemContext();
                Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
            });
        }

        TextView tvModelTitle = findViewById(R.id.tvModelTitle);
        if (tvModelTitle != null && currentTest != null && currentTest.getTitle() != null) {
            tvModelTitle.setText("ChatGPT 4o • " + currentTest.getTitle());
        }

        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages);
        rvChatMessages.setAdapter(chatAdapter);

        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendChat = findViewById(R.id.btnSendChat);
        llChatLoading = findViewById(R.id.llChatLoading);

        if (btnSendChat != null) {
            btnSendChat.setOnClickListener(v -> sendMessage());
        }

        // Setup Suggestion Chips
        Chip chipExplain = findViewById(R.id.chipSuggestExplain);
        Chip chipMistakes = findViewById(R.id.chipSuggestMistakes);
        Chip chipStrategy = findViewById(R.id.chipSuggestStrategy);
        Chip chipMotivate = findViewById(R.id.chipSuggestMotivate);

        if (chipExplain != null) {
            chipExplain.setOnClickListener(v -> sendPredefinedMessage("Explain Q1 in detail and clear my concept behind it."));
        }
        if (chipMistakes != null) {
            chipMistakes.setOnClickListener(v -> sendPredefinedMessage("Which questions did I answer wrong or skip, and why?"));
        }
        if (chipStrategy != null) {
            chipStrategy.setOnClickListener(v -> sendPredefinedMessage("Give me a step-by-step strategy to get a perfect score next time."));
        }
        if (chipMotivate != null) {
            chipMotivate.setOnClickListener(v -> sendPredefinedMessage("I feel demotivated by my score. Please motivate me and boost my confidence!"));
        }

        // Initialize Chat Context System Prompt
        initSystemContext();
    }

    private void initSystemContext() {
        int accuracy = totalCount > 0 ? (correctCount * 100) / totalCount : 0;
        String testTitle = (currentTest != null && currentTest.getTitle() != null) ? currentTest.getTitle() : "Mock Assessment";

        StringBuilder testDetails = new StringBuilder();
        testDetails.append("Test Title: ").append(testTitle).append("\n");
        testDetails.append("Score: ").append(correctCount).append("/").append(totalCount).append(" (Accuracy: ").append(accuracy).append("%)\n\n");
        testDetails.append("Questions Details:\n");

        if (currentTest != null && currentTest.getQuestions() != null) {
            int qSize = currentTest.getQuestions().size();
            for (int i = 0; i < qSize; i++) {
                Question q = currentTest.getQuestions().get(i);
                if (q == null) continue;

                boolean isCorrect = q.getSelectedOptionIndex() != null && Objects.equals(q.getSelectedOptionIndex(), q.getCorrectOptionIndex());
                String selectedOpt = "Not Attempted";
                if (q.getSelectedOptionIndex() != null && q.getOptions() != null && q.getSelectedOptionIndex() >= 0 && q.getSelectedOptionIndex() < q.getOptions().size()) {
                    selectedOpt = q.getOptions().get(q.getSelectedOptionIndex());
                }
                String correctOpt = "N/A";
                if (q.getOptions() != null && q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < q.getOptions().size()) {
                    correctOpt = q.getOptions().get(q.getCorrectOptionIndex());
                }

                testDetails.append("Q").append(i + 1).append(": ").append(q.getQuestionText() != null ? q.getQuestionText() : "")
                        .append("\n  - Student Answer: ").append(selectedOpt)
                        .append("\n  - Correct Answer: ").append(correctOpt)
                        .append("\n  - Result: ").append(isCorrect ? "CORRECT ✓" : "INCORRECT ✗");

                if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
                    testDetails.append("\n  - Explanation: ").append(q.getExplanation().trim());
                }
                testDetails.append("\n\n");
            }
        }

        String systemPrompt = "You are ChatGPT, an expert, friendly, and highly encouraging AI Exam Mentor for students preparing for competitive exams on Exam Uplift.\n" +
                "The student has just completed the test below and is chatting with you to clear doubts, understand questions one-by-one, learn core concepts, get smart tricks/shortcuts, get motivation, and improve their score.\n\n" +
                "--- TEST DATA --- \n" + testDetails.toString() + "\n--- END TEST DATA ---\n\n" +
                "Your Guidelines:\n" +
                "1. Answer every query accurately based on the test data above.\n" +
                "2. When explaining any question (wrong or correct), explain step-by-step AND give a practical SMART TRICK / SHORTCUT / MEMORY MNEMONIC (स्मार्ट ट्रिक / याद रखने की ट्रिक) to remember or solve it fast!\n" +
                "3. Respond in Hindi/Hinglish/English naturally based on what the student uses.\n" +
                "4. Be extremely supportive, motivating, and action-oriented.";

        apiHistoryMessages.add(new ChatRequest.Message("system", systemPrompt));

        // Add initial welcoming message
        String welcomeMsg = "👋 Hello! I am ChatGPT, your AI Exam Mentor.\n\nI have reviewed your test performance (**" + correctCount + "/" + totalCount + "** in " + testTitle + ").\n\nAsk me about any question (wrong or correct), and I will explain it step-by-step with a smart trick to remember or solve it fast!";
        chatMessages.add(new ChatMessage(welcomeMsg, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }

    private void sendPredefinedMessage(String text) {
        if (etChatMessage != null) {
            etChatMessage.setText(text);
            sendMessage();
        }
    }

    private void sendMessage() {
        if (etChatMessage == null) return;
        String userText = etChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) return;

        etChatMessage.setText("");

        // Add user message to UI
        chatMessages.add(new ChatMessage(userText, true));
        int pos = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(pos);
        if (rvChatMessages != null) {
            rvChatMessages.smoothScrollToPosition(pos);
        }

        // Add user message to API History
        apiHistoryMessages.add(new ChatRequest.Message("user", userText));

        // Show loading indicator
        if (llChatLoading != null) llChatLoading.setVisibility(View.VISIBLE);

        // Tier 1: Call OpenRouter API
        if (Constants.OPENROUTER_KEY != null && !Constants.OPENROUTER_KEY.trim().isEmpty()) {
            callOpenRouterChat(Constants.OPENROUTER_MODEL, userText);
        } else {
            // Tier 2: Call Pollinations AI
            callPollinationsChat(userText);
        }
    }

    private void callOpenRouterChat(String modelName, String userText) {
        ChatRequest request = new ChatRequest(modelName, new ArrayList<>(apiHistoryMessages));
        String authHeader = "Bearer " + Constants.OPENROUTER_KEY.trim();

        ApiClient.getOpenRouterInterface().getCompletion(
                authHeader,
                "https://examuplift.app",
                "Exam Uplift App",
                request
        ).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                if (isDestroyed() || isFinishing()) return;
                if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                    ChatResponse.Choice choice = response.body().choices.get(0);
                    if (choice != null && choice.message != null && !TextUtils.isEmpty(choice.message.content)) {
                        handleAiResponse(choice.message.content);
                        return;
                    }
                }

                // Try fallback model if main failed
                if (Objects.equals(modelName, Constants.OPENROUTER_MODEL) && Constants.OPENROUTER_FALLBACK_MODEL != null) {
                    callOpenRouterChat(Constants.OPENROUTER_FALLBACK_MODEL, userText);
                } else {
                    callPollinationsChat(userText);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                if (isDestroyed() || isFinishing()) return;
                if (Objects.equals(modelName, Constants.OPENROUTER_MODEL) && Constants.OPENROUTER_FALLBACK_MODEL != null) {
                    callOpenRouterChat(Constants.OPENROUTER_FALLBACK_MODEL, userText);
                } else {
                    callPollinationsChat(userText);
                }
            }
        });
    }

    private void callPollinationsChat(String userText) {
        List<PollinationsApi.PollinationsRequest.Message> pMessages = new ArrayList<>();
        for (ChatRequest.Message msg : apiHistoryMessages) {
            pMessages.add(new PollinationsApi.PollinationsRequest.Message(msg.role, msg.content));
        }

        PollinationsApi.PollinationsRequest req = new PollinationsApi.PollinationsRequest(pMessages);
        ApiClient.getPollinationsInterface().getCompletion(req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (isDestroyed() || isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        String result = body.string();
                        if (!TextUtils.isEmpty(result) && !result.toLowerCase().contains("error")) {
                            handleAiResponse(result);
                            return;
                        }
                    } catch (IOException ignored) {}
                }
                callRapidApiChat(userText);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (isDestroyed() || isFinishing()) return;
                callRapidApiChat(userText);
            }
        });
    }

    private void callRapidApiChat(String userText) {
        if (Constants.RAPIDAPI_KEY != null && !Constants.RAPIDAPI_KEY.isEmpty()) {
            List<RapidApiRequest.Message> messages = new ArrayList<>();
            messages.add(new RapidApiRequest.Message("user", userText));
            RapidApiRequest request = new RapidApiRequest(messages);

            ApiClient.getAIInterface().getCompletion(Constants.RAPIDAPI_HOST, Constants.RAPIDAPI_KEY, request)
                    .enqueue(new Callback<RapidApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<RapidApiResponse> call, @NonNull Response<RapidApiResponse> response) {
                            if (isDestroyed() || isFinishing()) return;
                            if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().result)) {
                                handleAiResponse(response.body().result);
                            } else {
                                handleAiResponse(generateSmartContextualFallback(userText));
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<RapidApiResponse> call, @NonNull Throwable t) {
                            if (isDestroyed() || isFinishing()) return;
                            handleAiResponse(generateSmartContextualFallback(userText));
                        }
                    });
        } else {
            handleAiResponse(generateSmartContextualFallback(userText));
        }
    }

    private String generateSmartContextualFallback(String userQuery) {
        if (userQuery == null) userQuery = "";
        String queryLower = userQuery.toLowerCase().trim();
        int accuracy = totalCount > 0 ? (correctCount * 100) / totalCount : 0;
        String testTitle = (currentTest != null && currentTest.getTitle() != null) ? currentTest.getTitle() : "Assessment";

        // Case 1: Explain Q1 or specific question
        if (queryLower.contains("explain q") || queryLower.contains("question") || queryLower.contains("q1") || queryLower.contains("q2")) {
            int qIndex = 0;
            if (queryLower.contains("q2")) qIndex = 1;
            else if (queryLower.contains("q3")) qIndex = 2;
            else if (queryLower.contains("q4")) qIndex = 3;
            else if (queryLower.contains("q5")) qIndex = 4;

            if (currentTest != null && currentTest.getQuestions() != null && !currentTest.getQuestions().isEmpty()) {
                if (qIndex >= currentTest.getQuestions().size()) qIndex = 0;
                Question q = currentTest.getQuestions().get(qIndex);
                if (q != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("💡 **Detailed Solution for Question ").append(qIndex + 1).append("**\n\n");
                    sb.append("**Question:** ").append(q.getQuestionText() != null ? q.getQuestionText() : "").append("\n\n");

                    if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                        sb.append("**Options:**\n");
                        for (int i = 0; i < q.getOptions().size(); i++) {
                            sb.append(i + 1).append(". ").append(q.getOptions().get(i)).append("\n");
                        }
                        sb.append("\n");
                    }

                    String correctOptText = "N/A";
                    if (q.getOptions() != null && q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < q.getOptions().size()) {
                        correctOptText = q.getOptions().get(q.getCorrectOptionIndex());
                    }
                    sb.append("✅ **Correct Answer:** Option ").append(q.getCorrectOptionIndex() + 1).append(" (").append(correctOptText).append(")\n\n");

                    if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
                        sb.append("📖 **Concept & Explanation:**\n").append(q.getExplanation().trim());
                    } else {
                        sb.append("📖 **Concept:** Review the key formulas and core principles related to this question type to solidify your understanding.");
                    }
                    return sb.toString();
                }
            }
        }

        // Case 2: Why wrong or mistakes
        if (queryLower.contains("wrong") || queryLower.contains("mistake") || queryLower.contains("skip") || queryLower.contains("incorrect")) {
            if (currentTest != null && currentTest.getQuestions() != null) {
                List<Question> wrongQuestions = new ArrayList<>();
                for (Question q : currentTest.getQuestions()) {
                    if (q != null) {
                        boolean isCorrect = q.getSelectedOptionIndex() != null && Objects.equals(q.getSelectedOptionIndex(), q.getCorrectOptionIndex());
                        if (!isCorrect) {
                            wrongQuestions.add(q);
                        }
                    }
                }

                if (!wrongQuestions.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("❓ **Review of Incorrect / Unattempted Questions (Total: ").append(wrongQuestions.size()).append(")**\n\n");
                    for (int i = 0; i < Math.min(wrongQuestions.size(), 5); i++) {
                        Question q = wrongQuestions.get(i);
                        String correctText = (q.getOptions() != null && q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < q.getOptions().size())
                                ? q.getOptions().get(q.getCorrectOptionIndex()) : "N/A";
                        sb.append("• **").append(q.getQuestionText()).append("**\n");
                        sb.append("  - Correct: ").append(correctText).append("\n");
                        if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
                            sb.append("  - Reason: ").append(q.getExplanation().trim()).append("\n");
                        }
                        sb.append("\n");
                    }
                    sb.append("💡 Focus on practicing these specific concepts again to avoid similar mistakes!");
                    return sb.toString();
                } else {
                    return "🎉 Great news! You scored 100% on this test with no incorrect answers! Keep up this amazing standard for your real exam!";
                }
            }
        }

        // Case 3: Strategy / 100% score
        if (queryLower.contains("strategy") || queryLower.contains("100%") || queryLower.contains("score") || queryLower.contains("perfect")) {
            StringBuilder sb = new StringBuilder();
            sb.append("🎯 **Step-by-Step Strategy for 100% Score in ").append(testTitle).append(":**\n\n");
            sb.append("1. **Analyze Errors:** Carefully read explanations for all incorrect & unattempted questions.\n");
            sb.append("2. **Time Management:** Spend no more than 45–60 seconds per question on first pass.\n");
            sb.append("3. **Formula Sheet:** Maintain a formula revision sheet for quick recap before tests.\n");
            sb.append("4. **Mock Re-attempt:** Re-take this mock test tomorrow to verify if your accuracy improved to 100%.");
            return sb.toString();
        }

        // Case 4: Motivation
        if (queryLower.contains("motivate") || queryLower.contains("confidence") || queryLower.contains("sad") || queryLower.contains("feel")) {
            StringBuilder sb = new StringBuilder();
            sb.append("🔥 **Keep Your Head High!**\n\n");
            sb.append("You scored **").append(correctCount).append("/").append(totalCount).append("** (").append(accuracy).append("% Accuracy) in ").append(testTitle).append(".\n\n");
            sb.append("Remember: Every mistake you make in a mock test is a mistake you **won't** make in the actual exam! Learn from these questions today and keep moving forward. Success is yours! 💪✨");
            return sb.toString();
        }

        // Default intelligent response
        return "I am ChatGPT, your AI mentor for **" + testTitle + "**.\n\nYou scored **" + correctCount + "/" + totalCount + "** (" + accuracy + "% accuracy).\n\nAsk me anything specific like:\n• *'Explain Q1 in detail'*\n• *'Why were my answers wrong?'*\n• *'Give me a strategy to score 100%'*\n• *'Motivate me!'*";
    }

    private void handleAiResponse(String aiReplyText) {
        if (llChatLoading != null) llChatLoading.setVisibility(View.GONE);

        // Add AI reply to history
        apiHistoryMessages.add(new ChatRequest.Message("assistant", aiReplyText));

        // Add to UI
        chatMessages.add(new ChatMessage(aiReplyText, false));
        int pos = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(pos);
        if (rvChatMessages != null) {
            rvChatMessages.smoothScrollToPosition(pos);
        }
    }
}