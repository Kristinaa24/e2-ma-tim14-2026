package com.tim14.slagalica;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AsocijacijeActivity extends AppCompatActivity {

    private TextView timerText;
    private Button btnA1, btnQuit;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asocijacije);

        timerText = findViewById(R.id.timerText);
        btnA1 = findViewById(R.id.btnA1);
        btnQuit = findViewById(R.id.btnQuitAsocijacije);

        // Simple Timer for GUI demo (KT1)
        timer = new CountDownTimer(120000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                timerText.setText("0s");
                Toast.makeText(AsocijacijeActivity.this, "Vreme je isteklo!", Toast.LENGTH_SHORT).show();
            }
        }.start();

        // Mock reveal for KT1
        btnA1.setOnClickListener(v -> {
            btnA1.setText("SRBIJA"); // Mock word
            btnA1.setBackgroundTintList(getResources().getColorStateList(R.color.slagalica_yellow));
        });

        // Quit functionality
        btnQuit.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}