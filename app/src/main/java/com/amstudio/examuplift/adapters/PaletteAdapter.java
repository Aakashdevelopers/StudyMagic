package com.amstudio.examuplift.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.R;
import com.amstudio.examuplift.models.Question;

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
        
        boolean isAnswered = q.getSelectedOptionIndex() != null;
        boolean isMarked = q.isMarkedForReview();
        boolean isVisited = q.isVisited();

        if (isAnswered && isMarked) {
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_answered_marked);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
            holder.ivStar.setVisibility(View.VISIBLE);
            holder.ivStar.setColorFilter(0xFFFFD54F);
        } else if (isAnswered) {
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_attempted);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
            holder.ivStar.setVisibility(View.GONE);
        } else if (isMarked) {
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_marked);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
            holder.ivStar.setVisibility(View.VISIBLE);
            holder.ivStar.setColorFilter(0xFFFFFFFF);
        } else if (isVisited) {
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_unattempted);
            holder.tvNumber.setTextColor(0xFFFFFFFF);
            holder.ivStar.setVisibility(View.GONE);
        } else {
            holder.tvNumber.setBackgroundResource(R.drawable.bg_palette_unseen);
            holder.tvNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textPrimary));
            holder.ivStar.setVisibility(View.GONE);
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