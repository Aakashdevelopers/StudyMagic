package com.amstudio.examuplift.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.R;
import com.amstudio.examuplift.models.Question;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

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
        
        if (q.getImageUrl() != null && !q.getImageUrl().trim().isEmpty()) {
            holder.ivQuestionImage.setVisibility(View.VISIBLE);
            Picasso.get().load(q.getImageUrl().trim()).into(holder.ivQuestionImage);
        } else {
            holder.ivQuestionImage.setVisibility(View.GONE);
        }

        List<String> opts = q.getOptions();
        String yourAns = "Not Attempted";
        if (q.getSelectedOptionIndex() != null && opts != null) {
            int selIdx = q.getSelectedOptionIndex();
            yourAns = formatOptionText(selIdx, opts);
        }
        holder.tvYourAns.setText("Your Answer: " + yourAns);

        String correctAns = "N/A";
        if (opts != null && !opts.isEmpty()) {
            int correctIdx = q.getCorrectOptionIndex();
            if (correctIdx >= 0 && correctIdx < opts.size()) {
                correctAns = formatOptionText(correctIdx, opts);
            } else if (correctIdx - 1 >= 0 && correctIdx - 1 < opts.size()) {
                correctAns = formatOptionText(correctIdx - 1, opts);
            } else {
                correctAns = "Option " + correctIdx;
            }
        }
        holder.tvCorrectAns.setText("Correct Answer: " + correctAns);

        String expText = q.getExplanation();
        String expImgUrl = q.getExplanationImageUrl();

        boolean hasExpText = expText != null && !expText.trim().isEmpty();
        boolean hasExpImg = expImgUrl != null && !expImgUrl.trim().isEmpty();

        if (hasExpText || hasExpImg) {
            holder.llExplanation.setVisibility(View.VISIBLE);

            if (hasExpText) {
                holder.tvExplanation.setVisibility(View.VISIBLE);
                holder.tvExplanation.setText(expText.trim());
            } else {
                holder.tvExplanation.setVisibility(View.GONE);
            }

            if (hasExpImg) {
                holder.ivExplanationImage.setVisibility(View.VISIBLE);
                String url = expImgUrl.trim().replace(" ", "%20");
                Picasso.get().load(url).into(holder.ivExplanationImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (holder.ivExplanationImage != null) {
                            holder.ivExplanationImage.post(() -> holder.ivExplanationImage.requestLayout());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        if (holder.ivExplanationImage != null) {
                            holder.ivExplanationImage.setVisibility(View.GONE);
                        }
                    }
                });
            } else {
                holder.ivExplanationImage.setVisibility(View.GONE);
            }
        } else {
            holder.llExplanation.setVisibility(View.GONE);
        }
    }

    private String formatOptionText(int idx, List<String> opts) {
        if (opts != null && idx >= 0 && idx < opts.size()) {
            String val = opts.get(idx);
            if (val != null) {
                String trimmed = val.trim();
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    return "Option " + (char) ('A' + idx) + " [Image]";
                }
                return val;
            }
        }
        return "Option " + (idx + 1);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvYourAns, tvCorrectAns, tvExplanation;
        ImageView ivQuestionImage, ivExplanationImage;
        View llExplanation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestionText);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
            tvYourAns = itemView.findViewById(R.id.tvYourAnswer);
            tvCorrectAns = itemView.findViewById(R.id.tvCorrectAnswer);
            tvExplanation = itemView.findViewById(R.id.tvExplanationText);
            ivExplanationImage = itemView.findViewById(R.id.ivExplanationImage);
            llExplanation = itemView.findViewById(R.id.llExplanation);
        }
    }
}