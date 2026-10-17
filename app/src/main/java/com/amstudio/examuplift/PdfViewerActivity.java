package com.amstudio.examuplift;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String PREF_NAME = "pdf_viewer_prefs";
    private static final String KEY_NIGHT_MODE = "is_night_mode";

    private RecyclerView pdfRecyclerView;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object pdfRendererLock = new Object();

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private File pdfFile;

    private PdfAdapter pdfAdapter;
    private boolean isNightMode = true;

    private final LruCache<Integer, Bitmap> bitmapCache;

    public PdfViewerActivity() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // 1/8th of app memory for bitmap cache
        bitmapCache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_pdf_viewer);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isNightMode = prefs.getBoolean(KEY_NIGHT_MODE, true);

        View main = findViewById(R.id.main);
        pdfRecyclerView = findViewById(R.id.pdfRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);

        pdfRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        updateThemeUI();

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
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
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_NIGHT_MODE, isNightMode)
                    .apply();
            item.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_night_mode);
            updateThemeUI();
            if (pdfAdapter != null) {
                pdfAdapter.notifyItemRangeChanged(0, pdfAdapter.getItemCount());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateThemeUI() {
        View main = findViewById(R.id.main);
        if (isNightMode) {
            if (main != null) main.setBackgroundColor(Color.BLACK);
            if (pdfRecyclerView != null) pdfRecyclerView.setBackgroundColor(Color.BLACK);
            if (toolbar != null) {
                toolbar.setBackgroundColor(Color.parseColor("#121212"));
                toolbar.setTitleTextColor(Color.WHITE);
                toolbar.setNavigationIconTint(Color.WHITE);
            }
            if (progressBar != null && progressBar.getIndeterminateDrawable() != null) {
                progressBar.getIndeterminateDrawable().setTint(Color.WHITE);
            }
            WindowInsetsUtil.setLightStatusBar(this, false);
        } else {
            if (main != null) main.setBackgroundColor(Color.WHITE);
            if (pdfRecyclerView != null) pdfRecyclerView.setBackgroundColor(Color.WHITE);
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
                    pdfFile = new File(getCacheDir(), "temp_pdf_document.pdf");
                    try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                         FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    mainHandler.post(this::initPdfRenderer);
                } else {
                    int code = responseCode;
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Failed to load PDF (Code: " + code + ")", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
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

    private void initPdfRenderer() {
        if (isFinishing() || isDestroyed()) return;
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "PDF content is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            if (parcelFileDescriptor != null) {
                synchronized (pdfRendererLock) {
                    pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                    pdfAdapter = new PdfAdapter(pdfRenderer.getPageCount());
                }
                pdfRecyclerView.setAdapter(pdfAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open PDF document", Toast.LENGTH_SHORT).show();
        } finally {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

        private final int pageCount;

        PdfAdapter(int pageCount) {
            this.pageCount = pageCount;
        }

        @NonNull
        @Override
        public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PdfViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) {
                currentPos = position;
            }
            final int pos = currentPos;

            holder.pageImageView.setImageBitmap(null);
            holder.pagePosition = pos;

            if (isNightMode) {
                ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
                holder.pageImageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            } else {
                holder.pageImageView.setColorFilter(null);
            }

            Bitmap cached = bitmapCache.get(pos);
            if (cached != null && !cached.isRecycled()) {
                holder.pageImageView.setImageBitmap(cached);
                return;
            }

            executorService.execute(() -> {
                Bitmap bitmap = renderPageBitmap(pos);
                if (bitmap != null) {
                    bitmapCache.put(pos, bitmap);
                    mainHandler.post(() -> {
                        if (holder.getBindingAdapterPosition() == pos) {
                            holder.pageImageView.setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return pageCount;
        }

        class PdfViewHolder extends RecyclerView.ViewHolder {
            final ImageView pageImageView;
            int pagePosition = -1;

            PdfViewHolder(@NonNull View itemView) {
                super(itemView);
                pageImageView = itemView.findViewById(R.id.pageImageView);
            }
        }
    }

    private Bitmap renderPageBitmap(int pageIndex) {
        synchronized (pdfRendererLock) {
            if (pdfRenderer == null) return null;
            PdfRenderer.Page page = null;
            try {
                page = pdfRenderer.openPage(pageIndex);
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int screenWidth = metrics.widthPixels;

                int pageWidth = page.getWidth();
                int pageHeight = page.getHeight();

                float scale = (float) screenWidth / (float) pageWidth;
                int renderHeight = (int) (pageHeight * scale);

                Bitmap bitmap = Bitmap.createBitmap(screenWidth, renderHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (page != null) {
                    try {
                        page.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        bitmapCache.evictAll();

        synchronized (pdfRendererLock) {
            try {
                if (pdfRenderer != null) {
                    pdfRenderer.close();
                    pdfRenderer = null;
                }
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                    parcelFileDescriptor = null;
                }
                if (pdfFile != null && pdfFile.exists()) {
                    if (!pdfFile.delete()) {
                        pdfFile.deleteOnExit();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}