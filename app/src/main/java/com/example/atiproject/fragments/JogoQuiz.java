package com.example.atiproject.fragments;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.atiproject.PandQ;
import com.example.atiproject.R;

import java.util.ArrayList;

public class JogoQuiz extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogo_quiz);


        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(JogoQuiz.this,modoJogar.class);
                startActivity(intent);
                finish();
            }
        }, 1500);
    }
}