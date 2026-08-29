package com.amstudio.studymagic.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import android.content.Intent;
import com.amstudio.studymagic.QuizActivity;
import com.amstudio.studymagic.adapters.TestAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.utils.MockData;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestListFragment extends Fragment {

    private TestAdapter adapter;
    private List<Test> testList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_list, container, false);

        String categoryId = getArguments() != null ? getArguments().getString("categoryId") : "";
        String topicId = getArguments() != null ? getArguments().getString("topicId") : "";
        String title = getArguments() != null ? getArguments().getString("topicName", getArguments().getString("categoryName", "Tests")) : "Tests";

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        toolbar.setTitle(title);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView rvTests = view.findViewById(R.id.rvTests);
        adapter = new TestAdapter(testList, test -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("test", test);
            startActivity(intent);
        });
        rvTests.setAdapter(adapter);

        if (topicId != null && !topicId.isEmpty()) {
            fetchTopicTests(topicId);
        } else if (categoryId != null && !categoryId.isEmpty()) {
            fetchCategoryTests(categoryId);
        }

        return view;
    }

    private void fetchTopicTests(String topicId) {
        ApiClient.getInterface().getTestsByTopic("eq." + topicId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    testList.clear();
                    for (SupabaseTest sTest : response.body()) {
                        testList.add(sTest.toTest());
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void fetchCategoryTests(String categoryId) {
        ApiClient.getInterface().getTestsByCategory("eq." + categoryId).enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    testList.clear();
                    for (SupabaseTest sTest : response.body()) {
                        testList.add(sTest.toTest());
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                // Handle error
            }
        });
    }
}