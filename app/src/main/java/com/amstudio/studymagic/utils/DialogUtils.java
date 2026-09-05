package com.amstudio.studymagic.utils;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.fragments.MockupListFragment;
import com.amstudio.studymagic.fragments.SubjectListFragment;
import com.amstudio.studymagic.models.Category;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {

    public static void showCategoryOptionsDialog(Context context, FragmentManager fragmentManager, Category category) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_category_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvCategoryName = dialogView.findViewById(R.id.tvDialogCategoryName);
        tvCategoryName.setText(category.getName());

        View cardMockTest = dialogView.findViewById(R.id.cardMockTest);
        View cardPracticeTest = dialogView.findViewById(R.id.cardPracticeTest);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        cardMockTest.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.getId());
            bundle.putString("categoryName", category.getName());

            MockupListFragment fragment = new MockupListFragment();
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        cardPracticeTest.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.getId());
            bundle.putString("categoryName", category.getName());

            SubjectListFragment fragment = new SubjectListFragment();
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
