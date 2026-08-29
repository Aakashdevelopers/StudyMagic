package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;

import java.util.List;

public class SubjectTabAdapter extends RecyclerView.Adapter<SubjectTabAdapter.ViewHolder> {

    private List<String> subjects;
    private OnSubjectClickListener listener;
    private String selectedSubject;

    public interface OnSubjectClickListener {
        void onSubjectClick(String subject);
    }

    public SubjectTabAdapter(List<String> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
        if (!subjects.isEmpty()) {
            this.selectedSubject = subjects.get(0);
        }
    }

    public void setSelectedSubject(String subject) {
        this.selectedSubject = subject;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String subject = subjects.get(position);
        holder.tvSubject.setText(subject);

        if (subject.equals(selectedSubject)) {
            holder.tvSubject.setBackgroundResource(R.drawable.bg_subject_tab_selected);
            holder.tvSubject.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.tvSubject.setBackgroundResource(R.drawable.bg_subject_tab_unselected);
            holder.tvSubject.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.textPrimary));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedSubject = subject;
            notifyDataSetChanged();
            listener.onSubjectClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubjectTab);
        }
    }
}
