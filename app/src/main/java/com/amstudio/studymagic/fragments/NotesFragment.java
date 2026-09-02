package com.amstudio.studymagic.fragments;

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
import com.amstudio.studymagic.PdfViewerActivity;
import com.amstudio.studymagic.R;
import com.amstudio.studymagic.adapters.NotesAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Note;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        View header = view.findViewById(R.id.llHeader);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(header);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false); // Light icons on dark header

        rvNotes = view.findViewById(R.id.rvNotes);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

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
                    if (notes.isEmpty()) {
                        Toast.makeText(getContext(), "No notes found in database", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load notes: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}