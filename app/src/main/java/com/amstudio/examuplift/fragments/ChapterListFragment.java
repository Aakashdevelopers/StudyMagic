package com.amstudio.examuplift.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.adapters.SimpleListAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.models.Chapter;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterListFragment extends Fragment {

    private SimpleListAdapter adapter;
    private List<Chapter> chapterList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_list, container, false);

        String subjectId = getArguments() != null ? getArguments().getString("subjectId") : "";
        String subjectName = getArguments() != null ? getArguments().getString("subjectName") : "Chapters";

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        WindowInsetsUtil.applyTopInset(toolbar);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        toolbar.setTitle(subjectName);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rvTests);
        adapter = new SimpleListAdapter(chapterList, item -> {
            Bundle bundle = new Bundle();
            bundle.putString("chapterId", item.getId());
            bundle.putString("chapterName", item.getName());
            
            TopicListFragment fragment = new TopicListFragment();
            fragment.setArguments(bundle);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rv.setAdapter(adapter);

        fetchChapters(subjectId);

        return view;
    }

    private void fetchChapters(String subjectId) {
        ApiClient.getInterface().getChapters("eq." + subjectId).enqueue(new Callback<List<Chapter>>() {
            @Override
            public void onResponse(Call<List<Chapter>> call, Response<List<Chapter>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chapterList.clear();
                    chapterList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Chapter>> call, Throwable t) {
                // Handle error
            }
        });
    }
}