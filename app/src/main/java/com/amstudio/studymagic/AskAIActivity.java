package com.amstudio.studymagic;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.adapters.ChatAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Question;
import com.amstudio.studymagic.models.ai.ChatMessage;
import com.amstudio.studymagic.models.ai.ChatRequest;
import com.amstudio.studymagic.models.ai.ChatResponse;
import com.amstudio.studymagic.utils.Constants;
import com.amstudio.studymagic.utils.WindowInsetsUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AskAIActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private View btnSend, btnLanguage, btnHistory;
    
    private Question initialQuestion;
    private String selectedLanguage = "English"; // Default language

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_ai);

        if (getIntent() != null && getIntent().hasExtra("question")) {
            initialQuestion = (Question) getIntent().getSerializableExtra("question");
        }

        initViews();
        
        if (initialQuestion != null) {
            startInitialConversation();
        } else {
            addAIMessage("Hello! I am your Study Magic Assistant. How can I help you today?");
        }
    }

    private void initViews() {
        View main = findViewById(R.id.main);
        View llTopBar = findViewById(R.id.llTopBar);
        View llChatInput = findViewById(R.id.llInputAreaContainer);
        
        // Set status bar icons to dark since background is white
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        // Apply insets manually to the root view
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            llTopBar.setPadding(llTopBar.getPaddingLeft(), systemBars.top, llTopBar.getPaddingRight(), llTopBar.getPaddingBottom());
            llChatInput.setPadding(llChatInput.getPaddingLeft(), llChatInput.getPaddingTop(), llChatInput.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnHistory = findViewById(R.id.btnHistory);

        chatAdapter = new ChatAdapter(messages);
        rvChat.setAdapter(chatAdapter);

        btnLanguage.setOnClickListener(v -> showLanguageDialog());
        
        btnHistory.setOnClickListener(v -> {
            messages.clear();
            chatAdapter.notifyDataSetChanged();
            addAIMessage("Chat history cleared. How can I help you?");
        });
        
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                addUserMessage(text);
                etMessage.setText("");
                sendMessageToAI(text);
            }
        });
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Hindi", "Hinglish"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Response Language");
        builder.setItems(languages, (dialog, which) -> {
            selectedLanguage = languages[which];
            Toast.makeText(this, "Language set to " + selectedLanguage, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void startInitialConversation() {
        String userQuery = "Explain this question briefly: " + initialQuestion.getQuestionText();
        addUserMessage(userQuery);
        
        String prompt = "You are a highly professional and encouraging Study Assistant, like a senior professor from a top institute. " +
                "A student is asking about this question in " + selectedLanguage + " language.\n" +
                "Question: " + initialQuestion.getQuestionText() + "\n" +
                "Options: " + initialQuestion.getOptions().toString() + "\n" +
                "Correct Answer: " + initialQuestion.getOptions().get(initialQuestion.getCorrectOptionIndex()) + "\n\n" +
                "Formatting Rules (CRITICAL):\n" +
                "1. DO NOT use LaTeX symbols like \\( or \\). Use simple text (e.g., use '3^2' instead of '\\(3^2\\)').\n" +
                "2. Use Markdown for emphasis: **bold** for important terms and *italic* for emphasis.\n" +
                "3. Use bullet points for steps to make it look clean.\n" +
                "4. Provide a creative 'Short Trick' or 'Mnemonic' to remember this.\n" +
                "5. Format the short trick inside [TRICK]...[/TRICK] tags.\n" +
                "6. Response must be in " + selectedLanguage + ".\n" +
                "7. Be concise but extremely clear. Keep it professional and academic.";
        
        sendMessageToAI(prompt);
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage("user", text, ChatMessage.TYPE_USER));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        rvChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAIMessage(String text) {
        messages.add(new ChatMessage("assistant", text, ChatMessage.TYPE_AI));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        rvChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void sendMessageToAI(String userText) {
        // Add a temporary thinking message
        ChatMessage thinkingMsg = new ChatMessage("assistant", "Thinking...", ChatMessage.TYPE_AI);
        messages.add(thinkingMsg);
        int thinkingPos = messages.size() - 1;
        chatAdapter.notifyItemInserted(thinkingPos);
        rvChat.smoothScrollToPosition(thinkingPos);

        List<ChatRequest.Message> apiMessages = new ArrayList<>();
        
        // System instructions as first message for professional behavior
        String systemInstruction = "You are a professional teacher. Use clean markdown formatting. " +
                "Avoid complex math symbols like LaTeX. Use simple text powers like ^2. " +
                "Explain everything clearly in " + selectedLanguage;
        apiMessages.add(new ChatRequest.Message("system", systemInstruction));

        // LIMIT HISTORY: Only send last 4 messages for maximum speed
        int start = Math.max(0, messages.size() - 5); 
        for (int i = start; i < messages.size() - 1; i++) {
            ChatMessage msg = messages.get(i);
            apiMessages.add(new ChatRequest.Message(msg.getRole(), msg.getContent()));
        }

        ChatRequest request = new ChatRequest(Constants.AI_MODEL, apiMessages);
        request.max_tokens = 500; 
        request.temperature = 0.6;

        ApiClient.getAIInterface().getCompletion("Bearer " + Constants.OPENROUTER_KEY, request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                        // Remove thinking message
                        messages.remove(thinkingPos);
                        chatAdapter.notifyItemRemoved(thinkingPos);

                        if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                            addAIMessage(response.body().choices.get(0).message.content);
                        } else {
                            String errorMsg = "Error: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    String errorDetail = response.errorBody().string();
                                    if (errorDetail.contains("\"message\":\"")) {
                                        errorMsg += " - " + errorDetail.split("\"message\":\"")[1].split("\"")[0];
                                    } else {
                                        errorMsg += " - " + errorDetail;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            addAIMessage("I encountered an error. " + errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        // Remove thinking message
                        messages.remove(thinkingPos);
                        chatAdapter.notifyItemRemoved(thinkingPos);
                        
                        addAIMessage("Check your internet connection. Error: " + t.getMessage());
                    }
                });
    }
}