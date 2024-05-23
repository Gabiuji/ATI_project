package com.example.atiproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.atiproject.R;
import com.google.android.material.snackbar.Snackbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class suporte extends AppCompatActivity {
    private CardView bt_help;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suporte);

        bt_help = findViewById(R.id.btn_ajuda);
        bt_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String zap = "https://wa.me/5592992510265";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(zap));
                startActivity(intent);
            }
        });
    }
}