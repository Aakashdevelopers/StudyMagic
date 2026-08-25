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
import com.amstudio.studymagic.QuizActivity;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.utils.MockData;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TestAdapter testAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Test> featuredTests = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

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
        testAdapter = new TestAdapter(featuredTests, test -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("test", test);
            startActivity(intent);
        });
        rvFeatured.setAdapter(testAdapter);

        fetchCategories();
        fetchFeaturedTests();

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

    private void fetchFeaturedTests() {
        ApiClient.getInterface().getTests().enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    featuredTests.clear();
                    for (SupabaseTest sTest : response.body()) {
                        featuredTests.add(sTest.toTest());
                    }
                    testAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                // Handle error
            }
        });
    }
}