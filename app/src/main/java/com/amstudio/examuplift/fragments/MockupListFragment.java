package com.amstudio.examuplift.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.amstudio.examuplift.MockupQuizActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.models.MockExam;
import com.amstudio.examuplift.models.MockSubcategory;
import com.amstudio.examuplift.models.SupabaseTest;
import com.amstudio.examuplift.models.Test;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MockupListFragment extends Fragment {

    private RecyclerView rvMockups;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private ImageView btnBackToExams;

    private List<MockExam> mockExamsList = new ArrayList<>();
    private List<MockSubcategory> mockSubcategoriesList = new ArrayList<>();
    private List<SupabaseTest> examMocksList = new ArrayList<>();

    private MockExamAdapter examAdapter;
    private MockSubcategoryAdapter subcategoryAdapter;
    private MockupAdapter mockAdapter;

    private MockExam selectedExam = null;
    private MockSubcategory selectedSubcategory = null;
    private OnBackPressedCallback onBackPressedCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mockups, container, false);

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        rvMockups = view.findViewById(R.id.rvMockups);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = view.findViewById(R.id.tvHeaderSubtitle);
        btnBackToExams = view.findViewById(R.id.btnBackToExams);

        rvMockups.setLayoutManager(new LinearLayoutManager(getContext()));

        examAdapter = new MockExamAdapter(mockExamsList, exam -> {
            selectedExam = exam;
            selectedSubcategory = null;
            showSubcategoriesListView();
        });

        subcategoryAdapter = new MockSubcategoryAdapter(mockSubcategoriesList, subcategory -> {
            selectedSubcategory = subcategory;
            showExamMocksView();
        });

        mockAdapter = new MockupAdapter(examMocksList, test -> {
            Intent intent = new Intent(getActivity(), MockupQuizActivity.class);
            intent.putExtra("supabaseTest", test);
            startActivity(intent);
        });

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (selectedSubcategory != null) {
                    selectedSubcategory = null;
                    showSubcategoriesListView();
                } else if (selectedExam != null) {
                    if (getArguments() != null && getArguments().containsKey("categoryId")) {
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    } else {
                        selectedExam = null;
                        showExamsListView();
                    }
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        btnBackToExams.setOnClickListener(v -> {
            if (selectedSubcategory != null) {
                selectedSubcategory = null;
                showSubcategoriesListView();
            } else if (selectedExam != null) {
                if (getArguments() != null && getArguments().containsKey("categoryId")) {
                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                } else {
                    selectedExam = null;
                    showExamsListView();
                }
            } else {
                showExamsListView();
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            if (selectedSubcategory != null) {
                fetchExamMocks(selectedSubcategory.getId(), selectedExam != null ? selectedExam.getId() : 0);
            } else if (selectedExam != null) {
                fetchMockSubcategories(selectedExam.getId());
            } else {
                fetchMockExams();
            }
        });

        String argCatId = getArguments() != null ? getArguments().getString("categoryId") : null;
        String argCatName = getArguments() != null ? getArguments().getString("categoryName") : null;

        if (argCatId != null && !argCatId.isEmpty()) {
            try {
                long cId = Long.parseLong(argCatId);
                selectedExam = new MockExam(cId, argCatName != null ? argCatName : "Exam Mocks", "", "");
                showSubcategoriesListView();
            } catch (NumberFormatException e) {
                showExamsListView();
            }
        } else {
            showExamsListView();
        }

        return view;
    }

    private void showExamsListView() {
        selectedExam = null;
        selectedSubcategory = null;
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Mock Test Exams");
        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText("Select an exam category to explore mock tests");
            tvHeaderSubtitle.setVisibility(View.VISIBLE);
        }
        if (btnBackToExams != null) btnBackToExams.setVisibility(View.GONE);
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(false);

        rvMockups.setAdapter(examAdapter);
        fetchMockExams();
    }

    private void showSubcategoriesListView() {
        if (selectedExam == null) {
            showExamsListView();
            return;
        }
        selectedSubcategory = null;
        if (tvHeaderTitle != null) tvHeaderTitle.setText(selectedExam.getTitle());
        if (tvHeaderSubtitle != null) {
            if (selectedExam.getDescription() != null && !selectedExam.getDescription().isEmpty()) {
                tvHeaderSubtitle.setText(selectedExam.getDescription());
                tvHeaderSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvHeaderSubtitle.setText("");
                tvHeaderSubtitle.setVisibility(View.GONE);
            }
        }
        if (btnBackToExams != null) btnBackToExams.setVisibility(View.VISIBLE);
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(true);

        rvMockups.setAdapter(subcategoryAdapter);
        fetchMockSubcategories(selectedExam.getId());
    }

    private void showExamMocksView() {
        if (selectedSubcategory == null) {
            if (selectedExam != null) {
                showSubcategoriesListView();
            } else {
                showExamsListView();
            }
            return;
        }
        if (tvHeaderTitle != null) tvHeaderTitle.setText(selectedSubcategory.getTitle());
        if (tvHeaderSubtitle != null) {
            if (selectedSubcategory.getDescription() != null && !selectedSubcategory.getDescription().isEmpty()) {
                tvHeaderSubtitle.setText(selectedSubcategory.getDescription());
                tvHeaderSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvHeaderSubtitle.setText("");
                tvHeaderSubtitle.setVisibility(View.GONE);
            }
        }
        if (btnBackToExams != null) btnBackToExams.setVisibility(View.VISIBLE);
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(true);

        rvMockups.setAdapter(mockAdapter);
        fetchExamMocks(selectedSubcategory.getId(), selectedExam != null ? selectedExam.getId() : 0);
    }

    private void fetchMockExams() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        ApiClient.getInterface().getMockExams().enqueue(new Callback<List<MockExam>>() {
            @Override
            public void onResponse(Call<List<MockExam>> call, Response<List<MockExam>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    mockExamsList.clear();
                    for (MockExam exam : response.body()) {
                        if (exam.isActive()) {
                            mockExamsList.add(exam);
                        }
                    }
                    examAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Failed to load mock exams", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MockExam>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMockSubcategories(long categoryId) {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        ApiClient.getInterface().getMockSubcategories("eq." + categoryId).enqueue(new Callback<List<MockSubcategory>>() {
            @Override
            public void onResponse(Call<List<MockSubcategory>> call, Response<List<MockSubcategory>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    mockSubcategoriesList.clear();
                    for (MockSubcategory sub : response.body()) {
                        if (sub.isActive()) {
                            mockSubcategoriesList.add(sub);
                        }
                    }
                    subcategoryAdapter.notifyDataSetChanged();
                } else {
                    fetchAllMockSubcategoriesFallback(categoryId);
                }
            }

            @Override
            public void onFailure(Call<List<MockSubcategory>> call, Throwable t) {
                fetchAllMockSubcategoriesFallback(categoryId);
            }
        });
    }

    private void fetchAllMockSubcategoriesFallback(long categoryId) {
        ApiClient.getInterface().getAllMockSubcategories().enqueue(new Callback<List<MockSubcategory>>() {
            @Override
            public void onResponse(Call<List<MockSubcategory>> call, Response<List<MockSubcategory>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                mockSubcategoriesList.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (MockSubcategory sub : response.body()) {
                        if (sub.isActive() && (sub.getCategoryId() == categoryId || categoryId <= 0)) {
                            mockSubcategoriesList.add(sub);
                        }
                    }
                    if (mockSubcategoriesList.isEmpty()) {
                        for (MockSubcategory sub : response.body()) {
                            if (sub.isActive()) {
                                mockSubcategoriesList.add(sub);
                            }
                        }
                    }
                }
                subcategoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<MockSubcategory>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Failed to load subcategories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchExamMocks(long subcategoryId, long categoryId) {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        final List<SupabaseTest> combinedList = new ArrayList<>();

        // 1. Fetch from mockup_tests by category_id (subcategoryId is stored in category_id)
        ApiClient.getInterface().getTestsByCategory("eq." + subcategoryId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (SupabaseTest st : response.body()) {
                        addUniqueTest(combinedList, st);
                    }
                }
                fetchExamMocksStep2(subcategoryId, categoryId, combinedList);
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                fetchExamMocksStep2(subcategoryId, categoryId, combinedList);
            }
        });
    }

    private void fetchExamMocksStep2(long subcategoryId, long categoryId, List<SupabaseTest> combinedList) {
        // 2. Fetch from mockup_tests by subcategory_id
        ApiClient.getInterface().getMockupTestsBySubcategory("eq." + subcategoryId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (SupabaseTest st : response.body()) {
                        addUniqueTest(combinedList, st);
                    }
                }
                fetchExamMocksStep3(subcategoryId, categoryId, combinedList);
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                fetchExamMocksStep3(subcategoryId, categoryId, combinedList);
            }
        });
    }

    private void fetchExamMocksStep3(long subcategoryId, long categoryId, List<SupabaseTest> combinedList) {
        // 3. Fetch from exam_mocks by subcategory_id
        ApiClient.getInterface().getExamMocksBySubcategory("eq." + subcategoryId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    for (SupabaseTest st : response.body()) {
                        addUniqueTest(combinedList, st);
                    }
                }

                if (!combinedList.isEmpty()) {
                    examMocksList.clear();
                    examMocksList.addAll(combinedList);
                    sortAndNotifyMocks();
                } else {
                    fetchExamMocksFallbackCategory(categoryId);
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (!combinedList.isEmpty()) {
                    examMocksList.clear();
                    examMocksList.addAll(combinedList);
                    sortAndNotifyMocks();
                } else {
                    fetchExamMocksFallbackCategory(categoryId);
                }
            }
        });
    }

    private void addUniqueTest(List<SupabaseTest> list, SupabaseTest item) {
        if (item == null) return;
        for (SupabaseTest existing : list) {
            if (existing.id == item.id) return;
        }
        list.add(item);
    }

    private void fetchExamMocksFallbackCategory(long categoryId) {
        if (categoryId <= 0) {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            examMocksList.clear();
            mockAdapter.notifyDataSetChanged();
            return;
        }
        ApiClient.getInterface().getExamMocksByExamType("eq." + categoryId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                examMocksList.clear();
                if (response.isSuccessful() && response.body() != null) {
                    examMocksList.addAll(response.body());
                    sortAndNotifyMocks();
                } else {
                    mockAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                examMocksList.clear();
                mockAdapter.notifyDataSetChanged();
            }
        });
    }

    private void sortAndNotifyMocks() {
        Collections.sort(examMocksList, (t1, t2) -> {
            String name1 = (t1 != null && t1.title != null) ? t1.title : "";
            String name2 = (t2 != null && t2.title != null) ? t2.title : "";
            return name1.compareToIgnoreCase(name2);
        });
        mockAdapter.notifyDataSetChanged();
    }

    // Adapter for Table 1: mock_exams (Category)
    private static class MockExamAdapter extends RecyclerView.Adapter<MockExamAdapter.ViewHolder> {
        private final List<MockExam> exams;
        private final OnExamClickListener listener;

        public interface OnExamClickListener {
            void onExamClick(MockExam exam);
        }

        public MockExamAdapter(List<MockExam> exams, OnExamClickListener listener) {
            this.exams = exams;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mock_exam, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MockExam exam = exams.get(position);
            holder.tvExamTitle.setText(exam.getTitle());
            if (exam.getDescription() != null && !exam.getDescription().isEmpty()) {
                holder.tvExamDescription.setText(exam.getDescription());
                holder.tvExamDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvExamDescription.setVisibility(View.GONE);
            }

            if (holder.ivExamIcon != null) {
                if (exam.getIconUrl() != null && !exam.getIconUrl().isEmpty()) {
                    Picasso.get()
                            .load(exam.getIconUrl())
                            .placeholder(R.drawable.ic_exam)
                            .error(R.drawable.ic_exam)
                            .into(holder.ivExamIcon);
                } else {
                    holder.ivExamIcon.setImageResource(R.drawable.ic_exam);
                }
            }

            holder.itemView.setOnClickListener(v -> listener.onExamClick(exam));
            if (holder.btnViewMocks != null) {
                holder.btnViewMocks.setOnClickListener(v -> listener.onExamClick(exam));
            }
        }

        @Override
        public int getItemCount() {
            return exams.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvExamTitle, tvExamDescription;
            ImageView ivExamIcon;
            View btnViewMocks;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvExamTitle = itemView.findViewById(R.id.tvExamTitle);
                tvExamDescription = itemView.findViewById(R.id.tvExamDescription);
                ivExamIcon = itemView.findViewById(R.id.ivExamIcon);
                btnViewMocks = itemView.findViewById(R.id.btnViewMocks);
            }
        }
    }

    // Adapter for Table 2: mock_subcategories (Subcategory)
    private static class MockSubcategoryAdapter extends RecyclerView.Adapter<MockSubcategoryAdapter.ViewHolder> {
        private final List<MockSubcategory> subcategories;
        private final OnSubcategoryClickListener listener;

        public interface OnSubcategoryClickListener {
            void onSubcategoryClick(MockSubcategory subcategory);
        }

        public MockSubcategoryAdapter(List<MockSubcategory> subcategories, OnSubcategoryClickListener listener) {
            this.subcategories = subcategories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mock_exam, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MockSubcategory subcategory = subcategories.get(position);
            holder.tvExamTitle.setText(subcategory.getTitle());
            if (subcategory.getDescription() != null && !subcategory.getDescription().isEmpty()) {
                holder.tvExamDescription.setText(subcategory.getDescription());
                holder.tvExamDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvExamDescription.setVisibility(View.GONE);
            }

            if (holder.ivExamIcon != null) {
                if (subcategory.getIconUrl() != null && !subcategory.getIconUrl().isEmpty()) {
                    Picasso.get()
                            .load(subcategory.getIconUrl())
                            .placeholder(R.drawable.ic_exam)
                            .error(R.drawable.ic_exam)
                            .into(holder.ivExamIcon);
                } else {
                    holder.ivExamIcon.setImageResource(R.drawable.ic_exam);
                }
            }

            holder.itemView.setOnClickListener(v -> listener.onSubcategoryClick(subcategory));
            if (holder.btnViewMocks != null) {
                holder.btnViewMocks.setOnClickListener(v -> listener.onSubcategoryClick(subcategory));
            }
        }

        @Override
        public int getItemCount() {
            return subcategories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvExamTitle, tvExamDescription;
            ImageView ivExamIcon;
            View btnViewMocks;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvExamTitle = itemView.findViewById(R.id.tvExamTitle);
                tvExamDescription = itemView.findViewById(R.id.tvExamDescription);
                ivExamIcon = itemView.findViewById(R.id.ivExamIcon);
                btnViewMocks = itemView.findViewById(R.id.btnViewMocks);
            }
        }
    }

    // Adapter for Table 3: exam_mocks (Mock Tests)
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
            if (holder.tvMarks != null) {
                Test t = test.toTest();
                int qCount = t.getQuestions() != null ? t.getQuestions().size() : 0;
                double maxMarks = t.getTotalMarks();
                String maxMarksStr = (maxMarks % 1 == 0) ? String.valueOf((long) maxMarks) : String.valueOf(maxMarks);
                if (test.negativeMarking != null && test.negativeMarking > 0) {
                    double neg = test.negativeMarking;
                    String negStr = (neg % 1 == 0) ? String.valueOf((long) neg) : String.valueOf(neg);
                    holder.tvMarks.setText(qCount > 0 ? (qCount + " Qs • " + maxMarksStr + " Marks (-" + negStr + " Neg)") : (maxMarksStr + " Marks (-" + negStr + " Neg)"));
                } else {
                    holder.tvMarks.setText(qCount > 0 ? (qCount + " Qs • " + maxMarksStr + " Marks") : (maxMarksStr + " Marks"));
                }
            }
            if (holder.btnStart != null) holder.btnStart.setOnClickListener(v -> listener.onItemClick(test));
            holder.itemView.setOnClickListener(v -> listener.onItemClick(test));
        }

        @Override
        public int getItemCount() {
            return tests.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription, tvDuration, tvMarks;
            View btnStart;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTestTitle);
                tvDescription = itemView.findViewById(R.id.tvTestDescription);
                tvDuration = itemView.findViewById(R.id.tvTestDuration);
                tvMarks = itemView.findViewById(R.id.tvTestMarks);
                btnStart = itemView.findViewById(R.id.btnStartTest);
            }
        }
    }
}
