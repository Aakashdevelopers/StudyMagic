package com.amstudio.studymagic;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.amstudio.studymagic.fragments.AboutDeveloperFragment;
import com.amstudio.studymagic.fragments.ExamsFragment;
import com.amstudio.studymagic.fragments.HomeFragment;
import com.amstudio.studymagic.fragments.NotesFragment;
import com.amstudio.studymagic.utils.WindowInsetsUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        WindowInsetsUtil.setLightStatusBar(this, true); // Dark icons for light background
        WindowInsetsUtil.applyBottomInset(findViewById(R.id.bottom_navigation));

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
            } else if (itemId == R.id.nav_about) {
                selectedFragment = new AboutDeveloperFragment();
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
}