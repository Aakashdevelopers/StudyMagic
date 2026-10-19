package com.amstudio.examuplift;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.amstudio.examuplift.fragments.ExamsFragment;
import com.amstudio.examuplift.fragments.HomeFragment;
import com.amstudio.examuplift.fragments.MockupListFragment;
import com.amstudio.examuplift.fragments.NotesFragment;
import com.amstudio.examuplift.utils.WindowInsetsUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 500;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        WindowInsetsUtil.setLightStatusBar(this, false); // Light icons (white) for dark headers
        WindowInsetsUtil.applyBottomInset(findViewById(R.id.bottom_navigation));

        // Check for Play Store In-App Updates
        checkForAppUpdate();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_exams) {
                selectedFragment = new ExamsFragment();
            } else if (itemId == R.id.nav_notes) {
                selectedFragment = new NotesFragment();
            } else if (itemId == R.id.nav_mockups) {
                selectedFragment = new MockupListFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void checkForAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("InAppUpdate", "Error starting update flow", e);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                this,
                                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                                UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("InAppUpdate", "Error resuming update flow", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.d("InAppUpdate", "Update flow failed or cancelled by user. Result code: " + resultCode);
            }
        }
    }
}