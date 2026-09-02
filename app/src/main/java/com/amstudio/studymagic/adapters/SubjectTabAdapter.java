package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubjectTabAdapter extends RecyclerView.Adapter<SubjectTabAdapter.ViewHolder> {

    private List<String> subjects;
    private OnSubjectClickListener listener;
    private String selectedSubject;
    private Set<String> lockedSubjects = new HashSet<>();
    private Set<String> completedSubjects = new HashSet<>();

    public interface OnSubjectClickListener {
        void onSubjectClick(String subject, boolean isLocked);
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

    public void setLockedSubjects(Set<String> lockedSubjects) {
        this.lockedSubjects = lockedSubjects != null ? lockedSubjects : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setCompletedSubjects(Set<String> completedSubjects) {
        this.completedSubjects = completedSubjects != null ? completedSubjects : new HashSet<>();
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

        boolean isSelected = subject.equals(selectedSubject);
        boolean isLocked = lockedSubjects.contains(subject);
        boolean isCompleted = completedSubjects.contains(subject);

        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.bg_subject_tab_selected);
            holder.tvSubject.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.ivLock.setVisibility(View.GONE);
            if (isCompleted) {
                holder.ivDone.setVisibility(View.VISIBLE);
                holder.ivDone.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else {
                holder.ivDone.setVisibility(View.GONE);
            }
        } else if (isLocked) {
            holder.container.setBackgroundResource(R.drawable.bg_subject_tab_locked);
            holder.tvSubject.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary));
            holder.ivLock.setVisibility(View.VISIBLE);
            holder.ivLock.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorError));
            holder.ivDone.setVisibility(View.GONE);
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_subject_tab_unselected);
            holder.tvSubject.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textPrimary));
            holder.ivLock.setVisibility(View.GONE);
            if (isCompleted) {
                holder.ivDone.setVisibility(View.VISIBLE);
                holder.ivDone.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSuccess));
            } else {
                holder.ivDone.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onSubjectClick(subject, isLocked));
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View container;
        TextView tvSubject;
        ImageView ivLock;
        ImageView ivDone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.llSubjectContainer);
            tvSubject = itemView.findViewById(R.id.tvSubjectTab);
            ivLock = itemView.findViewById(R.id.ivSubjectLock);
            ivDone = itemView.findViewById(R.id.ivSubjectDone);
        }
    }
}
