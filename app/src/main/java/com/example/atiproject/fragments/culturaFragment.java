package com.example.atiproject.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.atiproject.Dicionario;
import com.example.atiproject.R;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class culturaFragment extends Fragment {

    View vista;
    CardView btn_glossario, btn_dicionario, btn_cultura;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        vista = inflater.inflate(R.layout.fragment_cultura, container, false);
        btn_glossario = vista.findViewById(R.id.btn_glossario);
        btn_dicionario = vista.findViewById(R.id.bt_dicionario);
        btn_cultura = vista.findViewById(R.id.bt_cultura);
        btn_glossario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String web = "https://edoc.ufam.edu.br/bitstream/123456789/5989/1/Glossário%20lexical%20da%20língua%20Sateré-mawé%20%281%29.pdf";

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(web));
                startActivity(intent);
            }
        });
        btn_dicionario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), Dicionario.class);
                startActivity(intent);
            }
        });
        btn_cultura.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String web = "https://pib.socioambiental.org/pt/Povo:Sateré_Mawé";

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(web));
                startActivity(intent);
            }
        });
        return vista;
    }
}