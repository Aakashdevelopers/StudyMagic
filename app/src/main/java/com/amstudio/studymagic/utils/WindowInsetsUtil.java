package com.amstudio.studymagic.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.app.Activity;

public class WindowInsetsUtil {

    public static void applySystemBarInsets(View topView, View bottomView) {
        if (topView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(topView);
        }

        if (bottomView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(bottomView);
        }
    }

    public static void applyTopInset(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottomInset(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
    
    public static void handleImeInsets(View rootView, View scrollTarget) {
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeInsets.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    public static void setLightStatusBar(Activity activity, boolean isLight) {
        if (activity == null) return;
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(isLight);
    }
}
