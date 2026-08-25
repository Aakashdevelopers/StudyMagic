package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.Question;

import java.util.List;

public class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.ViewHolder> {

    private final List<Question> questions;
    private OnQuestionClickListener listener;

    public interface OnQuestionClickListener {
        void onQuestionClick(int index);
    }

    public PaletteAdapter(List<Question> questions, OnQuestionClickListener listener) {
        this.questions = questions;
        this.listener = listener;
    }

    public void setOnItemClickListener(OnQuestionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_palette_number, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = questions.get(position);
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.ivStar.setVisibility(View.GONE);
        
        boolean isAnswered = q.getSelectedOptionIndex() != null;
        boolean isMarked = q.isMarkedForReview();

        if (isAnswered && isMarked) {
            // Answered & Marked for Review (Purple)
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_answered_marked);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
            holder.ivStar.setVisibility(View.VISIBLE);
        } else if (isAnswered) {
            // Answered (Green/Primary)
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_attempted);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
        } else if (isMarked) {
            // Marked for Review (Gray with Star)
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_marked);
            holder.tvNumber.setTextColor(0xFF212121);
            holder.ivStar.setVisibility(View.VISIBLE);
        } else {
            // Unseen / Unattempted
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_unseen);
            holder.tvNumber.setTextColor(0xFF757575);
        }

        holder.itemView.setOnClickListener(v -> listener.onQuestionClick(position));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber;
        ImageView ivStar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvPaletteNumber);
            ivStar = itemView.findViewById(R.id.ivMarkedStar);
        }
    }
}