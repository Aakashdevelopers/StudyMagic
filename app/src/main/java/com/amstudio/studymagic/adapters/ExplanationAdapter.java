package com.amstudio.studymagic.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.AskAIActivity;
import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.Question;

import java.util.List;

public class ExplanationAdapter extends RecyclerView.Adapter<ExplanationAdapter.ViewHolder> {

    private final List<Question> questions;

    public ExplanationAdapter(List<Question> questions) {
        this.questions = questions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incorrect_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = questions.get(position);
        holder.tvQuestion.setText(q.getQuestionText());
        
        String yourAns = q.getSelectedOptionIndex() != null ? q.getOptions().get(q.getSelectedOptionIndex()) : "Not Attempted";
        holder.tvYourAns.setText("Your Answer: " + yourAns);
        holder.tvCorrectAns.setText("Correct Answer: " + q.getOptions().get(q.getCorrectOptionIndex()));

        holder.btnExplain.setText("✨ ASK AI");
        holder.btnExplain.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AskAIActivity.class);
            intent.putExtra("question", q);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvYourAns, tvCorrectAns;
        Button btnExplain;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestionText);
            tvYourAns = itemView.findViewById(R.id.tvYourAnswer);
            tvCorrectAns = itemView.findViewById(R.id.tvCorrectAnswer);
            btnExplain = itemView.findViewById(R.id.btnExplainAI);
        }
    }
}