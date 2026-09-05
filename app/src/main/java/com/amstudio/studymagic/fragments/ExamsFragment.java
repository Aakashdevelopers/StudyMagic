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
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamsFragment extends Fragment {

    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exams, container, false);

        View header = view.findViewById(R.id.llHeader);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(header);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        RecyclerView rvExams = view.findViewById(R.id.rvExams);
        categoryAdapter = new CategoryAdapter(categoryList, R.layout.item_category_grid, category -> {
            showCategoryOptionsDialog(category);
        });
        rvExams.setAdapter(categoryAdapter);

        fetchCategories();

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

    private void showCategoryOptionsDialog(Category category) {
        DialogUtils.showCategoryOptionsDialog(requireContext(), getParentFragmentManager(), category);
    }
}