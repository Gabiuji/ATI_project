package com.example.atiproject.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.atiproject.FormCadastro;
import com.example.atiproject.FormLogin;
import com.example.atiproject.PandQ;
import com.example.atiproject.R;
import com.example.atiproject.TelaPrincipal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class modoJogar extends AppCompatActivity implements View.OnClickListener {

    int i=0;
    int totalQ = 6;
    String selectedAns = "";
    TextView card_question, sairmdj;
    Button cardA, cardB, cardC, cardD;
    int correctCount=0;
    Button nextBtn;
    List<Integer> selectedQuestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_jogar);


        componentes();
        setALLdates();

        sairmdj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(modoJogar.this, TelaPrincipal.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

    }
    private void setALLdates() {
        if (selectedQuestions.size() == 5) { // Altere para o número desejado de perguntas
            finishQuiz();
            return;
        }

        int randomIndex;
        do {
            randomIndex = new Random().nextInt(totalQ);
        } while (selectedQuestions.contains(randomIndex));

        selectedQuestions.add(randomIndex);

        card_question.setText(PandQ.questions[randomIndex]);
        cardA.setText(PandQ.choices[randomIndex][0]);
        cardB.setText(PandQ.choices[randomIndex][1]);
        cardC.setText(PandQ.choices[randomIndex][2]);
        cardD.setText(PandQ.choices[randomIndex][3]);
    }



    private void finishQuiz() {
        String status = "";
        if(correctCount > totalQ * 0.6){
            status = "Muito Bem!";
        }else{
            status = "Pode melhorar";
        }

        new AlertDialog.Builder(this)
                .setTitle(status)
                .setMessage("Acertou "+correctCount+" de "+totalQ+ " questões")
                .setPositiveButton("Sair", ((dialogInterface, i1) -> SairdoQuiz()))
                .setCancelable(false)
                .show();

    }
    void SairdoQuiz(){
        Intent intent = new Intent(modoJogar.this, TelaPrincipal.class);
        startActivity(intent);
    }

    @SuppressLint("WrongViewCast")
    private void componentes() {
        //textos
        card_question = findViewById(R.id.card_question);
        //botoes
        cardA = findViewById(R.id.opA);
        cardB = findViewById(R.id.opB);
        cardC = findViewById(R.id.opC);
        cardD = findViewById(R.id.opD);
        nextBtn = findViewById(R.id.nextQuestion);
        sairmdj = findViewById(R.id.bt_sairmdj);

        cardA.setOnClickListener(this);
        cardB.setOnClickListener(this);
        cardC.setOnClickListener(this);
        cardD.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        cardA.setBackgroundColor(Color.WHITE);
        cardB.setBackgroundColor(Color.WHITE);
        cardC.setBackgroundColor(Color.WHITE);
        cardD.setBackgroundColor(Color.WHITE);

        Button clickedButton = (Button) view;
        if (clickedButton.getId() == R.id.nextQuestion) {
            if (!selectedAns.isEmpty()) {
                int currentQuestionIndex = selectedQuestions.get(selectedQuestions.size() - 1);
                if (selectedAns.equals(PandQ.correct[currentQuestionIndex])) {
                    correctCount++;
                }
            }

            setALLdates();
            selectedAns = "";
        } else {
            selectedAns = clickedButton.getText().toString();
            clickedButton.setBackgroundColor(Color.GREEN);
        }
    }
}