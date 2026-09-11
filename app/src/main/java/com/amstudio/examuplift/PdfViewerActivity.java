package com.amstudio.examuplift;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        
        setContentView(R.layout.activity_pdf_viewer);

        View main = findViewById(R.id.main);
        pdfView = findViewById(R.id.pdfView);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        
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

        if (pdfUrl != null && !pdfUrl.trim().isEmpty()) {
            loadPdfFromUrl(pdfUrl);
        } else {
            Toast.makeText(this, "PDF URL not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        MenuItem item = menu.findItem(R.id.action_night_mode);
        if (item != null) {
            item.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_night_mode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        if (urlString == null || urlString.trim().isEmpty()) {
            Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString.trim());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    String redirectUrl = connection.getHeaderField("Location");
                    if (redirectUrl != null) {
                        connection.disconnect();
                        url = new URL(redirectUrl);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(30000);
                        responseCode = connection.getResponseCode();
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    pdfBytes = outputStream.toByteArray();
                    runOnUiThread(() -> displayPdf(pdfBytes));
                } else {
                    int code = responseCode;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Failed to load PDF (Code: " + code + ")", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfViewerActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void displayPdf(byte[] bytes) {
        if (isFinishing() || isDestroyed()) return;
        if (bytes == null || bytes.length == 0) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "PDF content is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        View main = findViewById(R.id.main);
        if (isNightMode) {
            if (main != null) main.setBackgroundColor(Color.BLACK);
            if (toolbar != null) {
                toolbar.setBackgroundColor(Color.parseColor("#80000000"));
                toolbar.setTitleTextColor(Color.WHITE);
                toolbar.setNavigationIconTint(Color.WHITE);
            }
            if (progressBar != null && progressBar.getIndeterminateDrawable() != null) {
                progressBar.getIndeterminateDrawable().setTint(Color.WHITE);
            }
            WindowInsetsUtil.setLightStatusBar(this, false);
        } else {
            if (main != null) main.setBackgroundColor(Color.WHITE);
            if (toolbar != null) {
                toolbar.setBackgroundColor(Color.parseColor("#F8F9FA"));
                toolbar.setTitleTextColor(Color.BLACK);
                toolbar.setNavigationIconTint(Color.BLACK);
            }
            if (progressBar != null && progressBar.getIndeterminateDrawable() != null) {
                progressBar.getIndeterminateDrawable().setTint(Color.BLACK);
            }
            WindowInsetsUtil.setLightStatusBar(this, true);
        }

        try {
            pdfView.fromBytes(bytes)
                    .nightMode(isNightMode)
                    .onLoad(nbPages -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    })
                    .onError(t -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Error rendering PDF", Toast.LENGTH_SHORT).show();
                    })
                    .onPageError((page, t) -> {
                        Toast.makeText(PdfViewerActivity.this, "Error on page " + page, Toast.LENGTH_SHORT).show();
                    })
                    .load();
        } catch (Exception e) {
            e.printStackTrace();
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to open PDF viewer", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}