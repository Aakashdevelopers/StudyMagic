package com.amstudio.examuplift;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class WelcomeActivity extends AppCompatActivity {

    public static final String PREF_NAME = "examuplift_prefs";
    public static final String KEY_TERMS_ACCEPTED = "terms_accepted";

    private CheckBox cbAcceptTerms;
    private Button btnContinue;
    private TextView tvTermsLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        WindowInsetsUtil.setLightStatusBar(this, true);
        WindowInsetsUtil.applyTopInset(findViewById(R.id.welcome_root));

        cbAcceptTerms = findViewById(R.id.cbAcceptTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvTermsLink = findViewById(R.id.tvTermsLink);
        View cardConsentRow = findViewById(R.id.cardConsentRow);

        setupTermsText();

        if (cardConsentRow != null) {
            cardConsentRow.setOnClickListener(v -> cbAcceptTerms.setChecked(!cbAcceptTerms.isChecked()));
        }

        cbAcceptTerms.setOnCheckedChangeListener((buttonView, isChecked) -> updateContinueButton(isChecked));

        btnContinue.setOnClickListener(v -> {
            if (cbAcceptTerms.isChecked()) {
                acceptTermsAndProceed();
            } else {
                Toast.makeText(this, "Please accept the Terms & Conditions to proceed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTermsText() {
        String fullText = "I accept the Terms & Conditions and Privacy Policy.";
        SpannableString ss = new SpannableString(fullText);

        ClickableSpan termsClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyPolicyDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getColor(R.color.colorPrimary));
                ds.setUnderlineText(true);
            }
        };

        int start = fullText.indexOf("Terms & Conditions");
        int end = fullText.length() - 1; // Covers "Terms & Conditions and Privacy Policy"
        if (start >= 0) {
            ss.setSpan(termsClick, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTermsLink.setText(ss);
        tvTermsLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void updateContinueButton(boolean isChecked) {
        btnContinue.setEnabled(isChecked);
        btnContinue.setAlpha(isChecked ? 1.0f : 0.5f);
    }

    private void acceptTermsAndProceed() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply();

        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showPrivacyPolicyDialog() {
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
        
        // Make BottomSheet open expanded
        View parent = (View) dialogView.getParent();
        if (parent != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        dialog.show();
    }
}