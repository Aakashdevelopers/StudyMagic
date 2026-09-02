package com.amstudio.studymagic;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
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
    private MaterialToolbar toolbar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private byte[] pdfBytes;
    private boolean isNightMode = true;

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

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        android.view.MenuItem item = menu.findItem(R.id.action_night_mode);
        if (item != null) {
            item.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_night_mode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_night_mode) {
            isNightMode = !isNightMode;
            item.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_night_mode);
            if (pdfBytes != null) {
                displayPdf(pdfBytes);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadPdfFromUrl(String urlString) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    pdfBytes = outputStream.toByteArray();
                    runOnUiThread(() -> displayPdf(pdfBytes));
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfViewerActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayPdf(byte[] bytes) {
        View main = findViewById(R.id.main);
        if (isNightMode) {
            main.setBackgroundColor(android.graphics.Color.BLACK);
            toolbar.setBackgroundColor(android.graphics.Color.parseColor("#80000000"));
            toolbar.setTitleTextColor(android.graphics.Color.WHITE);
            toolbar.setNavigationIconTint(android.graphics.Color.WHITE);
            progressBar.getIndeterminateDrawable().setTint(android.graphics.Color.WHITE);
            WindowInsetsUtil.setLightStatusBar(this, false);
        } else {
            main.setBackgroundColor(android.graphics.Color.WHITE);
            toolbar.setBackgroundColor(android.graphics.Color.parseColor("#F8F9FA"));
            toolbar.setTitleTextColor(android.graphics.Color.BLACK);
            toolbar.setNavigationIconTint(android.graphics.Color.BLACK);
            progressBar.getIndeterminateDrawable().setTint(android.graphics.Color.BLACK);
            WindowInsetsUtil.setLightStatusBar(this, true);
        }

        pdfView.fromBytes(bytes)
                .nightMode(isNightMode)
                .onLoad(nbPages -> progressBar.setVisibility(View.GONE))
                .onError(t -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfViewerActivity.this, "Error loading PDF", Toast.LENGTH_SHORT).show();
                })
                .onPageError((page, t) -> {
                    Toast.makeText(PdfViewerActivity.this, "Error on page " + page, Toast.LENGTH_SHORT).show();
                })
                .load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}