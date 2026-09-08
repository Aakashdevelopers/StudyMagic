package com.amstudio.studymagic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.amstudio.studymagic.adapters.SearchAdapter;
import com.amstudio.studymagic.api.ApiClient;
import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.models.Note;
import com.amstudio.studymagic.models.SearchResult;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.utils.DialogUtils;
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private ImageView btnBackSearch, ivClearQuery;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipTests, chipNotes, chipCategories;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBarSearch;
    private View llInitialState, llEmptyState;

    private SearchAdapter adapter;
    private final List<SearchResult> allSearchItems = new ArrayList<>();
    private final List<SearchResult> filteredItems = new ArrayList<>();

    private boolean isTestsLoaded = false;
    private boolean isNotesLoaded = false;
    private boolean isCategoriesLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        View main = findViewById(R.id.main);
        View llSearchHeader = findViewById(R.id.llSearchHeader);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            llSearchHeader.setPadding(llSearchHeader.getPaddingLeft(), systemBars.top, llSearchHeader.getPaddingRight(), llSearchHeader.getPaddingBottom());
            return insets;
        });

        initViews();
        setupListeners();
        loadAllSearchData();
    }

    private void initViews() {
        btnBackSearch = findViewById(R.id.btnBackSearch);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        ivClearQuery = findViewById(R.id.ivClearQuery);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipTests = findViewById(R.id.chipTests);
        chipNotes = findViewById(R.id.chipNotes);
        chipCategories = findViewById(R.id.chipCategories);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        llInitialState = findViewById(R.id.llInitialState);
        llEmptyState = findViewById(R.id.llEmptyState);

        adapter = new SearchAdapter(filteredItems, result -> {
            onItemClicked(result);
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        etSearchQuery.requestFocus();
        etSearchQuery.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void setupListeners() {
        btnBackSearch.setOnClickListener(v -> finish());

        ivClearQuery.setOnClickListener(v -> {
            etSearchQuery.setText("");
            performFilter("");
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                ivClearQuery.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                performFilter(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            performFilter(etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "");
        });
    }

    private void loadAllSearchData() {
        progressBarSearch.setVisibility(View.VISIBLE);

        // Fetch Tests
        ApiClient.getInterface().getAllMockupTests().enqueue(new Callback<List<SupabaseTest>>() {
            @Override
            public void onResponse(Call<List<SupabaseTest>> call, Response<List<SupabaseTest>> response) {
                isTestsLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    for (SupabaseTest test : response.body()) {
                        String title = test.title != null ? test.title : "Mock Test";
                        String sub = test.duration > 0 ? test.duration + " Mins • Full Exam" : "Mock Test";
                        allSearchItems.add(new SearchResult(SearchResult.Type.TEST, title, sub, test));
                    }
                }
                checkDataLoaded();
            }

            @Override
            public void onFailure(Call<List<SupabaseTest>> call, Throwable t) {
                isTestsLoaded = true;
                checkDataLoaded();
            }
        });

        // Fetch Notes
        ApiClient.getInterface().getNotes().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                isNotesLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    for (Note note : response.body()) {
                        String title = note.title != null ? note.title : "Study Note";
                        String sub = note.description != null && !note.description.trim().isEmpty() ? note.description : "PDF Study Document";
                        allSearchItems.add(new SearchResult(SearchResult.Type.NOTE, title, sub, note));
                    }
                }
                checkDataLoaded();
            }

            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                isNotesLoaded = true;
                checkDataLoaded();
            }
        });

        // Fetch Categories
        ApiClient.getInterface().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                isCategoriesLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    for (Category cat : response.body()) {
                        String title = cat.getName() != null ? cat.getName() : "Category";
                        allSearchItems.add(new SearchResult(SearchResult.Type.CATEGORY, title, "Exam Category", cat));
                    }
                }
                checkDataLoaded();
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                isCategoriesLoaded = true;
                checkDataLoaded();
            }
        });
    }

    private void checkDataLoaded() {
        if (isTestsLoaded && isNotesLoaded && isCategoriesLoaded) {
            progressBarSearch.setVisibility(View.GONE);
            performFilter(etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "");
        }
    }

    private void performFilter(String query) {
        filteredItems.clear();

        if (query.isEmpty()) {
            adapter.updateList(filteredItems);
            llInitialState.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
            return;
        }

        llInitialState.setVisibility(View.GONE);
        String lowerQuery = query.toLowerCase();

        boolean allowTests = chipAll.isChecked() || chipTests.isChecked();
        boolean allowNotes = chipAll.isChecked() || chipNotes.isChecked();
        boolean allowCategories = chipAll.isChecked() || chipCategories.isChecked();

        for (SearchResult item : allSearchItems) {
            if (item.getType() == SearchResult.Type.TEST && !allowTests) continue;
            if (item.getType() == SearchResult.Type.NOTE && !allowNotes) continue;
            if (item.getType() == SearchResult.Type.CATEGORY && !allowCategories) continue;

            boolean matchesTitle = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerQuery);
            boolean matchesSub = item.getSubtitle() != null && item.getSubtitle().toLowerCase().contains(lowerQuery);

            if (matchesTitle || matchesSub) {
                filteredItems.add(item);
            }
        }

        adapter.updateList(filteredItems);
        llEmptyState.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onItemClicked(SearchResult result) {
        if (result == null || result.getData() == null) return;

        if (result.getType() == SearchResult.Type.TEST && result.getData() instanceof SupabaseTest) {
            Intent intent = new Intent(this, MockupQuizActivity.class);
            intent.putExtra("supabaseTest", (SupabaseTest) result.getData());
            startActivity(intent);
        } else if (result.getType() == SearchResult.Type.NOTE && result.getData() instanceof Note) {
            Note note = (Note) result.getData();
            if (note.pdfUrl != null && !note.pdfUrl.trim().isEmpty()) {
                Intent intent = new Intent(this, PdfViewerActivity.class);
                intent.putExtra("pdf_url", note.pdfUrl);
                intent.putExtra("title", note.title != null ? note.title : "Note");
                startActivity(intent);
            } else {
                Toast.makeText(this, "PDF URL not found", Toast.LENGTH_SHORT).show();
            }
        } else if (result.getType() == SearchResult.Type.CATEGORY && result.getData() instanceof Category) {
            Category cat = (Category) result.getData();
            DialogUtils.showCategoryOptionsDialog(this, getSupportFragmentManager(), cat);
        }
    }
}
