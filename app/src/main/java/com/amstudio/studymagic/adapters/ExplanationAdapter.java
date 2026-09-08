package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvQuestion.setText(q.getQuestionText() != null ? q.getQuestionText() : "");
        
        List<String> opts = q.getOptions();
        String yourAns = "Not Attempted";
        if (q.getSelectedOptionIndex() != null && opts != null) {
            int selIdx = q.getSelectedOptionIndex();
            if (selIdx >= 0 && selIdx < opts.size()) {
                yourAns = opts.get(selIdx);
            } else {
                yourAns = "Option " + (selIdx + 1);
            }
        }
        holder.tvYourAns.setText("Your Answer: " + yourAns);

        String correctAns = "N/A";
        if (opts != null && !opts.isEmpty()) {
            int correctIdx = q.getCorrectOptionIndex();
            if (correctIdx >= 0 && correctIdx < opts.size()) {
                correctAns = opts.get(correctIdx);
            } else if (correctIdx - 1 >= 0 && correctIdx - 1 < opts.size()) {
                correctAns = opts.get(correctIdx - 1);
            } else {
                correctAns = "Option " + correctIdx;
            }
        }
        holder.tvCorrectAns.setText("Correct Answer: " + correctAns);

        if (q.explanation != null && !q.explanation.isEmpty()) {
            holder.llExplanation.setVisibility(View.VISIBLE);
            holder.tvExplanation.setText(q.explanation);
        } else {
            holder.llExplanation.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvYourAns, tvCorrectAns, tvExplanation;
        View llExplanation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestionText);
            tvYourAns = itemView.findViewById(R.id.tvYourAnswer);
            tvCorrectAns = itemView.findViewById(R.id.tvCorrectAnswer);
            tvExplanation = itemView.findViewById(R.id.tvExplanationText);
            llExplanation = itemView.findViewById(R.id.llExplanation);
        }
    }
}
