package com.example.atiproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class Dicionario extends AppCompatActivity {

    private SearchView searchView;
    private LinearLayout resultsLayout;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dicionario);

        searchView = findViewById(R.id.searchView);
        resultsLayout = findViewById(R.id.resultsLayout);

        // Configurar o SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                pesquisarPalavra(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Não é necessário implementar nada aqui para a funcionalidade de pesquisa em tempo real
                return false;
            }
        });

        // Inicializar o Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("dicionario");
    }
    private void pesquisarPalavra(String query) {
        String idiomaOrigem = "Português";  // Defina o idioma de origem como "Português"
        String idiomaDestino = "Indígena";  // Defina o idioma de destino como "Indígena"

        Query searchQuery = databaseRef.orderByChild("traducao").startAt(query).endAt(query + "\uf8ff");

        searchQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                resultsLayout.removeAllViews();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Palavra palavra = snapshot.getValue(Palavra.class);

                    if (palavra.getTraducao().equalsIgnoreCase(query)) {
                        TextView textView = new TextView(Dicionario.this);
                        textView.setText(palavra.getTraducao() + " - " + palavra.getPalavraOriginal());
                        textView.setGravity(Gravity.CENTER);  // Centralizar o texto verticalmente e horizontalmente

                        // Adicionar a TextView ao layout de resultados
                        resultsLayout.addView(textView);
                    }
                }

                // Verificar se nenhum resultado foi encontrado
                if (resultsLayout.getChildCount() == 0) {
                    // Exibir uma mensagem de "Nenhum resultado encontrado" ou realizar outra ação apropriada
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Tratar o erro de leitura do banco de dados, se necessário
            }
        });
    }


}