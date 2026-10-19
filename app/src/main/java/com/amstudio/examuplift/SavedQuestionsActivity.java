package com.amstudio.examuplift;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.models.Question;
import com.amstudio.examuplift.utils.SavedQuestionsManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SavedQuestionsActivity extends AppCompatActivity {

    private RecyclerView rvSavedQuestions;
    private LinearLayout llEmptyState;
    private TextView tvSavedCount;
    private View btnClearAll;
    private SavedAdapter adapter;
    private List<Question> savedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_questions);

        View main = findViewById(R.id.main);
        View llHeader = findViewById(R.id.llHeader);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            llHeader.setPadding(llHeader.getPaddingLeft(), systemBars.top, llHeader.getPaddingRight(), llHeader.getPaddingBottom());
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvSavedQuestions = findViewById(R.id.rvSavedQuestions);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvSavedCount = findViewById(R.id.tvSavedCount);
        btnClearAll = findViewById(R.id.btnClearAll);

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> showClearAllConfirmationDialog());
        }

        rvSavedQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedAdapter();
        rvSavedQuestions.setAdapter(adapter);

        loadQuestions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQuestions();
    }

    private void loadQuestions() {
        savedList = SavedQuestionsManager.getSavedQuestions(this);
        updateSavedCountUI();
        if (savedList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvSavedQuestions.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvSavedQuestions.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void updateSavedCountUI() {
        tvSavedCount.setText(savedList.size() + " Saved Question" + (savedList.size() == 1 ? "" : "s"));
        if (btnClearAll != null) {
            btnClearAll.setVisibility(savedList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void showClearAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Saved Questions?")
                .setMessage("Are you sure you want to remove all saved questions? This action cannot be undone.")
                .setPositiveButton("Clear All / हटाएँ", (dialog, which) -> {
                    SavedQuestionsManager.clearAllSavedQuestions(SavedQuestionsActivity.this);
                    loadQuestions();
                    Toast.makeText(SavedQuestionsActivity.this, "All saved questions cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel / रद्द करें", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDeleteQuestionConfirmationDialog(Question toRemove, int adapterPos) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Saved Question?")
                .setMessage("Are you sure you want to remove this question from your saved list?")
                .setPositiveButton("Remove / हटाएँ", (dialog, which) -> {
                    SavedQuestionsManager.removeQuestion(SavedQuestionsActivity.this, toRemove);
                    if (adapterPos >= 0 && adapterPos < savedList.size()) {
                        savedList.remove(adapterPos);
                        adapter.notifyItemRemoved(adapterPos);
                        adapter.notifyItemRangeChanged(adapterPos, savedList.size());
                    }
                    updateSavedCountUI();
                    Toast.makeText(SavedQuestionsActivity.this, "Question removed", Toast.LENGTH_SHORT).show();
                    if (savedList.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvSavedQuestions.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel / रद्द करें", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_question, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Question q = savedList.get(position);
            holder.tvTag.setText("Question #" + (position + 1));
            holder.tvQuestion.setText(q.getQuestionText() != null ? q.getQuestionText() : "");

            if (q.getImageUrl() != null && !q.getImageUrl().trim().isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                Picasso.get().load(q.getImageUrl().trim().replace(" ", "%20")).into(holder.ivImage);
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }

            // Options
            holder.llOptions.removeAllViews();
            List<String> options = q.getOptions();
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    TextView tvOpt = new TextView(SavedQuestionsActivity.this);
                    boolean isCorrect = (i == q.getCorrectOptionIndex());

                    String optText = (i + 1) + ". " + options.get(i) + (isCorrect ? "  ✔ (Correct)" : "");
                    tvOpt.setText(optText);
                    tvOpt.setTextSize(14f);
                    tvOpt.setPadding(24, 20, 24, 20);

                    if (isCorrect) {
                        tvOpt.setTextColor(0xFF10B981);
                        tvOpt.setBackgroundResource(R.drawable.bg_marks_badge);
                    } else {
                        tvOpt.setTextColor(0xFFE5E7EB);
                    }

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 8, 0, 8);
                    tvOpt.setLayoutParams(params);

                    holder.llOptions.addView(tvOpt);
                }
            }

            // Explanation
            if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
                holder.llExplanation.setVisibility(View.VISIBLE);
                holder.tvExplanation.setText(q.getExplanation().trim());
            } else {
                holder.llExplanation.setVisibility(View.GONE);
            }

            // Delete click
            holder.btnDelete.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && adapterPos < savedList.size()) {
                    Question toRemove = savedList.get(adapterPos);
                    showDeleteQuestionConfirmationDialog(toRemove, adapterPos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return savedList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTag, tvQuestion, tvExplanation;
            ImageView ivImage, btnDelete;
            LinearLayout llOptions, llExplanation;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTag = itemView.findViewById(R.id.tvSavedQuestionTag);
                tvQuestion = itemView.findViewById(R.id.tvQuestionText);
                tvExplanation = itemView.findViewById(R.id.tvExplanationText);
                ivImage = itemView.findViewById(R.id.ivQuestionImage);
                btnDelete = itemView.findViewById(R.id.btnDeleteSaved);
                llOptions = itemView.findViewById(R.id.llOptionsContainer);
                llExplanation = itemView.findViewById(R.id.llExplanationContainer);
            }
        }
    }
}
