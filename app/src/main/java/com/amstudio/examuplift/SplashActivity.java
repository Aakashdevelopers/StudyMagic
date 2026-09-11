package com.amstudio.examuplift;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.amstudio.examuplift.utils.WindowInsetsUtil;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        WindowInsetsUtil.setLightStatusBar(this, false);
        WindowInsetsUtil.applyBottomInset(findViewById(R.id.tv_splash_version));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean termsAccepted = getSharedPreferences(WelcomeActivity.PREF_NAME, MODE_PRIVATE)
                    .getBoolean(WelcomeActivity.KEY_TERMS_ACCEPTED, false);

            Intent intent;
            if (termsAccepted) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}