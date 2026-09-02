package com.amstudio.studymagic.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.adapters.CategoryAdapter;
import com.amstudio.studymagic.adapters.CategoryAdapter;
import com.amstudio.studymagic.adapters.TestAdapter;
import android.content.Intent;
import com.amstudio.studymagic.MockupQuizActivity;
import com.amstudio.studymagic.QuizActivity;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.utils.MockData;
import com.amstudio.studymagic.utils.WindowInsetsUtil;

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
        WindowInsetsUtil.setLightStatusBar(getActivity(), false); // Light icons on dark header

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.getId());
            bundle.putString("categoryName", category.getName());
            
            SubjectListFragment fragment = new SubjectListFragment();
            fragment.setArguments(bundle);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvCategories.setAdapter(categoryAdapter);

        RecyclerView rvFeatured = view.findViewById(R.id.rvFeaturedTests);
        // Custom logic for Home Featured Tests to handle MockupQuizActivity
        testAdapter = new TestAdapter(new ArrayList<>(), test -> {}); // Dummy for layout manager if needed, but we'll use a better way
        
        // Let's create a specialized adapter or just update the existing one to handle SupabaseTest
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
}