package com.amstudio.studymagic;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        
        // Prevent screenshots and screen recording
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        
        setContentView(R.layout.activity_pdf_viewer);

        View main = findViewById(R.id.main);
        pdfView = findViewById(R.id.pdfView);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        
        // Set status bar icons to light since toolbar/background is dark
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });

        String pdfUrl = getIntent().getStringExtra("pdf_url");
        String title = getIntent().getStringExtra("title");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Notes");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (pdfUrl != null) {
            loadPdfFromUrl(pdfUrl);
        } else {
            Toast.makeText(this, "PDF URL not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPdfFromUrl(String urlString) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    runOnUiThread(() -> {
                        pdfView.fromStream(inputStream)
                                .onLoad(nbPages -> progressBar.setVisibility(View.GONE))
                                .onError(t -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(PdfViewerActivity.this, "Error loading PDF", Toast.LENGTH_SHORT).show();
                                })
                                .onPageError((page, t) -> {
                                    Toast.makeText(PdfViewerActivity.this, "Error on page " + page, Toast.LENGTH_SHORT).show();
                                })
                                .load();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfViewerActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}