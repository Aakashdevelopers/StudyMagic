package com.amstudio.examuplift.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.R;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_TEST = "arg_test";
    private static final String ARG_CORRECT = "arg_correct";
    private static final String ARG_TOTAL = "arg_total";

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

    public static AiChatBottomSheetDialogFragment newInstance(Test test, int correct, int total) {
        AiChatBottomSheetDialogFragment fragment = new AiChatBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TEST, test);
        args.putInt(ARG_CORRECT, correct);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, 0);
        if (getArguments() != null) {
            currentTest = (Test) getArguments().getSerializable(ARG_TEST);
            correctCount = getArguments().getInt(ARG_CORRECT, 0);
            totalCount = getArguments().getInt(ARG_TOTAL, 0);
        }
        chatMessages = new ArrayList<>();
        apiHistoryMessages = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                bottomSheet.getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ai_chat, container, false);

        View btnClose = view.findViewById(R.id.btnCloseChat);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        TextView tvTitle = view.findViewById(R.id.tvChatTitle);
        if (tvTitle != null && currentTest != null && currentTest.getTitle() != null) {
            tvTitle.setText("AI Mentor • " + currentTest.getTitle());
        }

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatAdapter = new ChatAdapter(chatMessages);
        rvChatMessages.setAdapter(chatAdapter);

        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnSendChat = view.findViewById(R.id.btnSendChat);
        llChatLoading = view.findViewById(R.id.llChatLoading);

        if (btnSendChat != null) {
            btnSendChat.setOnClickListener(v -> sendMessage());
        }

        // Setup Suggestion Chips
        Chip chipExplain = view.findViewById(R.id.chipSuggestExplain);
        Chip chipMistakes = view.findViewById(R.id.chipSuggestMistakes);
        Chip chipStrategy = view.findViewById(R.id.chipSuggestStrategy);
        Chip chipMotivate = view.findViewById(R.id.chipSuggestMotivate);

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

        return view;
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

        String systemPrompt = "You are an expert, friendly, and highly encouraging AI Exam Mentor for students preparing for competitive exams on Exam Uplift.\n" +
                "The student has just completed the test below and is chatting with you to clear doubts, understand questions, learn concepts, get motivation, and improve their score.\n\n" +
                "--- TEST DATA --- \n" + testDetails.toString() + "\n--- END TEST DATA ---\n\n" +
                "Your Guidelines:\n" +
                "1. Answer every query accurately based on the test data above.\n" +
                "2. Respond in Hindi/Hinglish/English naturally based on what the student uses.\n" +
                "3. Explain solutions step-by-step with simple examples when asked about any question.\n" +
                "4. Be extremely supportive, motivating, and action-oriented.";

        apiHistoryMessages.add(new ChatRequest.Message("system", systemPrompt));

        // Add initial welcoming message
        String welcomeMsg = "👋 Hello! I am your AI Mentor.\n\nI have reviewed your score (**" + correctCount + "/" + totalCount + "** in " + testTitle + ").\n\nHow can I help you? Ask me any doubt about any question, concepts, or ask for motivation!";
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
        rvChatMessages.smoothScrollToPosition(pos);

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
                if (!isAdded()) return;
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
                if (!isAdded()) return;
                if (Objects.equals(modelName, Constants.OPENROUTER_MODEL) && Constants.OPENROUTER_FALLBACK_MODEL != null) {
                    callOpenRouterChat(Constants.OPENROUTER_FALLBACK_MODEL, userText);
                } else {
                    callPollinationsChat(userText);
                }
            }
        });
    }

    private void callPollinationsChat(String userText) {
        StringBuilder promptBuilder = new StringBuilder();
        for (ChatRequest.Message msg : apiHistoryMessages) {
            promptBuilder.append(msg.role.toUpperCase()).append(": ").append(msg.content).append("\n\n");
        }

        PollinationsApi.PollinationsRequest req = new PollinationsApi.PollinationsRequest(promptBuilder.toString());
        ApiClient.getPollinationsInterface().getCompletion(req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        String result = body.string();
                        if (!TextUtils.isEmpty(result)) {
                            handleAiResponse(result);
                            return;
                        }
                    } catch (IOException ignored) {}
                }
                callRapidApiChat(userText);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
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
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().result)) {
                                handleAiResponse(response.body().result);
                            } else {
                                handleAiResponse("I am here to support you! Keep revising the incorrect questions and practice regularly to excel in your next attempt!");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<RapidApiResponse> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            handleAiResponse("I am here to support you! Keep revising the incorrect questions and practice regularly to excel in your next attempt!");
                        }
                    });
        } else {
            handleAiResponse("I am here to support you! Keep revising the incorrect questions and practice regularly to excel in your next attempt!");
        }
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
