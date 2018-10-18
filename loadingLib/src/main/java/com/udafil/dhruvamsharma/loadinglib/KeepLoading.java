package com.udafil.dhruvamsharma.loadinglib;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class KeepLoading extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keep_loading);


        TextView loadingTextView = findViewById(R.id.loading_text_tv);

        loadingTextView.setText(R.string.app_name);

    }
}
