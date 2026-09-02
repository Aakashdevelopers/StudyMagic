package com.amstudio.studymagic.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.amstudio.studymagic.R;
import com.amstudio.studymagic.adapters.SimpleListAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Subject;
import com.amstudio.studymagic.utils.MockData;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectListFragment extends Fragment {

    private SimpleListAdapter adapter;
    private List<Subject> subjectList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_list, container, false);

        String categoryId = getArguments() != null ? getArguments().getString("categoryId") : "";
        String categoryName = getArguments() != null ? getArguments().getString("categoryName") : "Subjects";

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);
        
        toolbar.setTitle(categoryName);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rvTests);
        adapter = new SimpleListAdapter(subjectList, item -> {
            Bundle bundle = new Bundle();
            bundle.putString("subjectId", item.getId());
            bundle.putString("subjectName", item.getName());
            
            ChapterListFragment fragment = new ChapterListFragment();
            fragment.setArguments(bundle);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rv.setAdapter(adapter);

        fetchSubjects(categoryId);

        return view;
    }

    private void fetchSubjects(String categoryId) {
        ApiClient.getInterface().getSubjects("eq." + categoryId).enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subjectList.clear();
                    subjectList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                // Handle error
            }
        });
    }
}
