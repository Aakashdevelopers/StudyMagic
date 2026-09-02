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
import com.amstudio.studymagic.models.Topic;
import com.amstudio.studymagic.utils.MockData;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicListFragment extends Fragment {

    private SimpleListAdapter adapter;
    private List<Topic> topicList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_list, container, false);

        String chapterId = getArguments() != null ? getArguments().getString("chapterId") : "";
        String chapterName = getArguments() != null ? getArguments().getString("chapterName") : "Topics";

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(toolbar);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        toolbar.setTitle(chapterName);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rvTests);
        adapter = new SimpleListAdapter(topicList, item -> {
            Bundle bundle = new Bundle();
            bundle.putString("topicId", item.getId());
            bundle.putString("topicName", item.getName());
            
            TestListFragment fragment = new TestListFragment();
            fragment.setArguments(bundle);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rv.setAdapter(adapter);

        fetchTopics(chapterId);

        return view;
    }

    private void fetchTopics(String chapterId) {
        ApiClient.getInterface().getTopics("eq." + chapterId).enqueue(new Callback<List<Topic>>() {
            @Override
            public void onResponse(Call<List<Topic>> call, Response<List<Topic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    topicList.clear();
                    topicList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Topic>> call, Throwable t) {
                // Handle error
            }
        });
    }
}
