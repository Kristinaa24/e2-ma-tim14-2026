package com.tim14.slagalica;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SkockoActivity extends AppCompatActivity {

    private Button btnQuit;
    private LinearLayout statusBarLayout;

    private boolean isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skocko);

        btnQuit = findViewById(R.id.btnQuitSkocko);
        statusBarLayout = findViewById(R.id.statusBarLayout);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        if (isGuest) {
            statusBarLayout.setVisibility(View.GONE);
        }

        btnQuit.setOnClickListener(v -> finish());
    }
}