package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.Test;

import java.util.List;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder> {

    private final List<Test> tests;
    private final OnTestClickListener listener;

    public interface OnTestClickListener {
        void onTestClick(Test test);
    }

    public TestAdapter(List<Test> tests, OnTestClickListener listener) {
        this.tests = tests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Test test = tests.get(position);
        holder.tvTitle.setText(test.getTitle());
        holder.tvDescription.setText(test.getDescription());
        holder.tvDuration.setText(test.getDurationMinutes() + " Mins");
        holder.btnStart.setOnClickListener(v -> listener.onTestClick(test));
        holder.itemView.setOnClickListener(v -> listener.onTestClick(test));
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDuration;
        Button btnStart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTestTitle);
            tvDescription = itemView.findViewById(R.id.tvTestDescription);
            tvDuration = itemView.findViewById(R.id.tvTestDuration);
            btnStart = itemView.findViewById(R.id.btnStartTest);
        }
    }
}