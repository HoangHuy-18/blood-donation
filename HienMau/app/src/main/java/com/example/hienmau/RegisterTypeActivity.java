package com.example.hienmau;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterTypeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_type);

        findViewById(R.id.cardPersonal).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterPersonalActivity.class));
        });

        findViewById(R.id.cardMedical).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterMedicalActivity.class));
        });
    }
}
