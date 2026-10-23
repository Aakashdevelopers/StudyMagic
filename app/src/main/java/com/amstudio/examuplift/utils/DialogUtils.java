package com.amstudio.examuplift.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.amstudio.examuplift.R;
import com.amstudio.examuplift.api.ApiClient;
import com.amstudio.examuplift.fragments.MockupListFragment;
import com.amstudio.examuplift.fragments.SubjectListFragment;
import com.amstudio.examuplift.fragments.TestListFragment;
import com.amstudio.examuplift.models.AppFeedback;
import com.amstudio.examuplift.models.Category;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DialogUtils {

    public static void showCategoryOptionsDialog(Context context, FragmentManager fragmentManager, Category category) {
        if (context == null || fragmentManager == null || category == null) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_category_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvCategoryName = dialogView.findViewById(R.id.tvDialogCategoryName);
        if (tvCategoryName != null && category.getName() != null) {
            tvCategoryName.setText(category.getName());
        }

        View cardMockTest = dialogView.findViewById(R.id.cardMockTest);
        View cardPracticeTest = dialogView.findViewById(R.id.cardPracticeTest);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (cardMockTest != null) {
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
        }

        if (cardPracticeTest != null) {
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
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public static void showFeedbackDialog(Context context) {
        if (context == null) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarFeedback);
        TextInputEditText etReview = dialogView.findViewById(R.id.etReview);
        View btnCancel = dialogView.findViewById(R.id.btnCancelFeedback);
        View btnSubmit = dialogView.findViewById(R.id.btnSubmitFeedback);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                int rating = ratingBar != null ? Math.round(ratingBar.getRating()) : 0;
                String review = etReview != null && etReview.getText() != null ? etReview.getText().toString().trim() : "";

                if (rating <= 0) {
                    Toast.makeText(context, "Please select a rating / कृपया रेटिंग चुनें", Toast.LENGTH_SHORT).show();
                    return;
                }

                AppFeedback feedback = new AppFeedback(rating, review, "", "");
                Toast.makeText(context, "Sending feedback...", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                ApiClient.getInterface().sendFeedback(feedback).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Thank you! Your feedback has been received. / धन्यवाद! आपका फीडबैक मिल गया है।", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "Feedback sent! / फीडबैक सबमिट हो गया है।", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(context, "Thank you! Your feedback has been recorded.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }
}