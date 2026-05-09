package com.tim14.slagalica;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AsocijacijeActivity extends AppCompatActivity {

    private TextView timerText;
    private Button btnA1, btnQuit;
    private LinearLayout statusBarLayout;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asocijacije);

        timerText = findViewById(R.id.timerText);
        btnA1 = findViewById(R.id.btnA1);
        btnQuit = findViewById(R.id.btnQuitAsocijacije);
        statusBarLayout = findViewById(R.id.statusBarLayout);

        boolean isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        if (isGuest) {
            statusBarLayout.setVisibility(View.GONE);
        }

        timer = new CountDownTimer(120000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText(millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                timerText.setText("0s");
                Toast.makeText(AsocijacijeActivity.this, "Vreme je isteklo!", Toast.LENGTH_SHORT).show();
            }
        }.start();

        btnA1.setOnClickListener(v -> {
            btnA1.setText("SRBIJA");
            btnA1.setBackgroundTintList(getResources().getColorStateList(R.color.slagalica_yellow));
        });

        btnQuit.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timer != null) {
            timer.cancel();
        }
    }
}