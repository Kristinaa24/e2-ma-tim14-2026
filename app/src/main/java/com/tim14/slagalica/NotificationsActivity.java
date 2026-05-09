package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Button btnBack = findViewById(R.id.btnBackFromNotif);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish()); // Go back to Home
        }
    }
}