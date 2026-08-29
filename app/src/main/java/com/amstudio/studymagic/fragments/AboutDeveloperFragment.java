package com.amstudio.studymagic.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.amstudio.studymagic.R;
import com.google.android.material.button.MaterialButton;

public class AboutDeveloperFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_developer, container, false);

        View header = view.findViewById(R.id.llHeader);
        com.amstudio.studymagic.utils.WindowInsetsUtil.applyTopInset(header);
        com.amstudio.studymagic.utils.WindowInsetsUtil.setLightStatusBar(getActivity(), false);

        MaterialButton btnGithub = view.findViewById(R.id.btnGithub);
        MaterialButton btnLinkedIn = view.findViewById(R.id.btnLinkedIn);
        MaterialButton btnEmail = view.findViewById(R.id.btnEmail);

        btnGithub.setOnClickListener(v -> openUrl("https://github.com/harish-computer"));
        btnLinkedIn.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/harish-computer/"));
        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@studymagic.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Query regarding Study Magic App");
            startActivity(Intent.createChooser(intent, "Send Email"));
        });

        return view;
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}