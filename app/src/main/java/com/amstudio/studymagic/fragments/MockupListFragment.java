package com.amstudio.studymagic.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.amstudio.studymagic.MockupQuizActivity;
import com.amstudio.studymagic.R;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockupListFragment extends Fragment {

    private RecyclerView rvMockups;
    private MockupAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private List<SupabaseTest> mockupList = new ArrayList<>();
    private String filterCategoryId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mockups, container, false);

        if (getArguments() != null) {
            filterCategoryId = getArguments().getString("categoryId");
        }

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        rvMockups = view.findViewById(R.id.rvMockups);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        rvMockups.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MockupAdapter(mockupList, test -> {
            Intent intent = new Intent(getActivity(), MockupQuizActivity.class);
            intent.putExtra("supabaseTest", test);
            startActivity(intent);
        });
        rvMockups.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchMockups);
        fetchMockups();

        return view;
    }

    private void fetchMockups() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        ApiClient.getInterface().getAllMockupTests().enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    mockupList.clear();
                    for (SupabaseTest test : response.body()) {
                        if (filterCategoryId == null || filterCategoryId.isEmpty() ||
                                String.valueOf(test.categoryId).equals(String.valueOf(filterCategoryId))) {
                            mockupList.add(test);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Failed to load mockups", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class MockupAdapter extends RecyclerView.Adapter<MockupAdapter.ViewHolder> {
        private final List<SupabaseTest> tests;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(SupabaseTest test);
        }

        public MockupAdapter(List<SupabaseTest> tests, OnItemClickListener listener) {
            this.tests = tests;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SupabaseTest test = tests.get(position);
            if (holder.tvTitle != null) holder.tvTitle.setText(test.title);
            if (holder.tvDescription != null) holder.tvDescription.setText(test.description);
            if (holder.tvDuration != null) holder.tvDuration.setText(test.duration + " Mins");
            if (holder.btnStart != null) holder.btnStart.setOnClickListener(v -> listener.onItemClick(test));
            holder.itemView.setOnClickListener(v -> listener.onItemClick(test));
        }

        @Override
        public int getItemCount() {
            return tests.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription, tvDuration;
            View btnStart;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTestTitle);
                tvDescription = itemView.findViewById(R.id.tvTestDescription);
                tvDuration = itemView.findViewById(R.id.tvTestDuration);
                btnStart = itemView.findViewById(R.id.btnStartTest);
            }
        }
    }
}