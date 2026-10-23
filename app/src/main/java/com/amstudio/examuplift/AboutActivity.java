package com.amstudio.examuplift;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.examuplift.utils.DialogUtils;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AboutActivity extends AppCompatActivity {

    public static final String TELEGRAM_MAIN_URL = "https://t.me/examuplift";
    public static final String TELEGRAM_DOUBTS_URL = "https://t.me/mathsandreasoningdoubts";
    public static final String YOUTUBE_URL = "https://youtube.com/@examuplift?si=v0T28EsV_xgYOcFv";
    public static final String INSTAGRAM_URL = "https://www.instagram.com/exam_uplift?mdxt=dzBoZDhzNThneDZn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        View appBarLayout = findViewById(R.id.appBarLayout);
        WindowInsetsUtil.applyTopInset(appBarLayout != null ? appBarLayout : findViewById(R.id.topBar));

        View scrollView = findViewById(R.id.scrollView);
        WindowInsetsUtil.applyBottomInset(scrollView);

        WindowInsetsUtil.setLightStatusBar(this, false);

        // Version setup
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (tvAppVersion != null) {
                tvAppVersion.setText("Version " + pInfo.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (tvAppVersion != null) {
                tvAppVersion.setText("Version 1.0.0");
            }
        }

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Share App button
        View btnShareApp = findViewById(R.id.btnShareApp);
        if (btnShareApp != null) {
            btnShareApp.setOnClickListener(v -> shareAppDetails());
        }

        // 1. Telegram Main Channel Button / Card
        View cardTelegram = findViewById(R.id.cardTelegram);
        View btnTelegramJoin = findViewById(R.id.btnTelegramJoin);
        View.OnClickListener telegramListener = v -> openUrl(TELEGRAM_MAIN_URL);
        if (cardTelegram != null) cardTelegram.setOnClickListener(telegramListener);
        if (btnTelegramJoin != null) btnTelegramJoin.setOnClickListener(telegramListener);

        // 2. Maths & Reasoning Doubts Group Button / Card
        View cardMathsDoubts = findViewById(R.id.cardMathsDoubts);
        View btnDoubtsAsk = findViewById(R.id.btnDoubtsAsk);
        View.OnClickListener doubtsListener = v -> openUrl(TELEGRAM_DOUBTS_URL);
        if (cardMathsDoubts != null) cardMathsDoubts.setOnClickListener(doubtsListener);
        if (btnDoubtsAsk != null) btnDoubtsAsk.setOnClickListener(doubtsListener);

        // 3. YouTube Channel Button / Card
        View cardYoutube = findViewById(R.id.cardYoutube);
        View btnYoutubeSubscribe = findViewById(R.id.btnYoutubeSubscribe);
        View.OnClickListener youtubeListener = v -> openUrl(YOUTUBE_URL);
        if (cardYoutube != null) cardYoutube.setOnClickListener(youtubeListener);
        if (btnYoutubeSubscribe != null) btnYoutubeSubscribe.setOnClickListener(youtubeListener);

        // 4. Instagram Page Button / Card
        View cardInstagram = findViewById(R.id.cardInstagram);
        View btnInstagramFollow = findViewById(R.id.btnInstagramFollow);
        View.OnClickListener instagramListener = v -> openUrl(INSTAGRAM_URL);
        if (cardInstagram != null) cardInstagram.setOnClickListener(instagramListener);
        if (btnInstagramFollow != null) btnInstagramFollow.setOnClickListener(instagramListener);

        // Feedback Button
        View btnFeedback = findViewById(R.id.btnFeedback);
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> DialogUtils.showFeedbackDialog(this));
        }

        // Privacy Policy Button
        View btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyBottomSheet());
        }
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Could not open link / लिंक नहीं खोला जा सका", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error opening link", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareAppDetails() {
        String shareBody = "Join EXAM UPLIFT for competitive exam preparation, free test series & doubts solution! 🚀\n\n"
                + "📢 Telegram Channel: " + TELEGRAM_MAIN_URL + "\n"
                + "💡 Maths & Reasoning Doubts: " + TELEGRAM_DOUBTS_URL + "\n"
                + "📺 Free Lectures YouTube: " + YOUTUBE_URL + "\n"
                + "📸 Instagram Page: " + INSTAGRAM_URL + "\n\n"
                + "Download Exam Uplift App for free mock tests & study materials!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "EXAM UPLIFT - Study App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share EXAM UPLIFT via"));
    }

    private void showPrivacyPolicyBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_privacy_policy_sheet, null);

        WebView webView = dialogView.findViewById(R.id.webViewPrivacy);
        View btnClose = dialogView.findViewById(R.id.btnCloseSheet);

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.loadUrl("file:///android_asset/privacy_policy.html");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setContentView(dialogView);

        View parent = (View) dialogView.getParent();
        if (parent != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        dialog.show();
    }
}