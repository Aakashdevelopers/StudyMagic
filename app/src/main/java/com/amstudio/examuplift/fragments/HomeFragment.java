package com.amstudio.examuplift.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.MockupQuizActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.SearchActivity;
import com.amstudio.examuplift.adapters.CategoryAdapter;
import com.amstudio.examuplift.adapters.TestAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.models.Category;
import com.amstudio.examuplift.models.SupabaseTest;
import com.amstudio.examuplift.utils.DialogUtils;
import com.amstudio.examuplift.utils.WindowInsetsUtil;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TestAdapter testAdapter;
    private CategoryAdapter categoryAdapter;
    private List<SupabaseTest> featuredTests = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        View llSearchBar = view.findViewById(R.id.llSearchBar);
        if (llSearchBar != null) {
            llSearchBar.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            });
        }

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            showCategoryOptionsDialog(category);
        });
        rvCategories.setAdapter(categoryAdapter);

        RecyclerView rvFeatured = view.findViewById(R.id.rvFeaturedTests);
        rvFeatured.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
                return new TestAdapter.ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                SupabaseTest sTest = featuredTests.get(position);
                TestAdapter.ViewHolder vh = (TestAdapter.ViewHolder) holder;
                vh.tvTitle.setText(sTest.title);
                vh.tvDescription.setText(sTest.description);
                vh.tvDuration.setText(sTest.duration + " Mins");
                
                View.OnClickListener listener = v -> {
                    Intent intent = new Intent(getActivity(), MockupQuizActivity.class);
                    intent.putExtra("supabaseTest", sTest);
                    startActivity(intent);
                };
                
                vh.btnStart.setOnClickListener(listener);
                vh.itemView.setOnClickListener(listener);
            }

            @Override
            public int getItemCount() {
                return featuredTests.size();
            }
        });

        fetchCategories();
        fetchFeaturedTests(view);

        return view;
    }

    private void fetchCategories() {
        ApiClient.getInterface().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void fetchFeaturedTests(View rootView) {
        ApiClient.getInterface().getFeaturedTests().enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    featuredTests.clear();
                    featuredTests.addAll(response.body());
                    RecyclerView rv = rootView.findViewById(R.id.rvFeaturedTests);
                    if (rv != null && rv.getAdapter() != null) {
                        rv.getAdapter().notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void showCategoryOptionsDialog(Category category) {
        DialogUtils.showCategoryOptionsDialog(requireContext(), getParentFragmentManager(), category);
    }
}