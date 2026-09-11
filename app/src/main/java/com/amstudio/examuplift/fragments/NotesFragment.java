package com.amstudio.examuplift.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.amstudio.examuplift.PdfViewerActivity;
import com.amstudio.examuplift.R;
import com.amstudio.examuplift.adapters.NotesAdapter;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.models.Note;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotesFragment extends Fragment {

    private RecyclerView rvNotes;
    private NotesAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        View header = view.findViewById(R.id.llHeader);
        WindowInsetsUtil.applyTopInset(header);
        WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        rvNotes = view.findViewById(R.id.rvNotes);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateView = view.findViewById(R.id.llEmptyState);

        rvNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotesAdapter(new ArrayList<>(), note -> {
            Intent intent = new Intent(getActivity(), PdfViewerActivity.class);
            intent.putExtra("pdf_url", note.pdfUrl);
            intent.putExtra("title", note.title);
            startActivity(intent);
        });
        rvNotes.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchNotes);

        fetchNotes();

        return view;
    }

    private void fetchNotes() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        ApiClient.getInterface().getNotes().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Note> notes = response.body();
                    adapter.updateList(notes);
                    if (emptyStateView != null) {
                        emptyStateView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } else {
                    if (emptyStateView != null) {
                        emptyStateView.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(getContext(), "Failed to load notes: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (emptyStateView != null) {
                    emptyStateView.setVisibility(View.VISIBLE);
                }
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}