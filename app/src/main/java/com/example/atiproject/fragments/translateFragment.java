package com.example.atiproject.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.atiproject.Palavra;
import com.example.atiproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class translateFragment extends Fragment {
    private Spinner fromSpin, toSpin;
    private TextInputEditText source;
    private MaterialButton bt_traduzir;
    private TextView traduzido;
    View vista;
    private FloatingActionButton audio, reproduzirAudio;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private List<Palavra> listaPalavrasFirebase;
    private boolean listaPalavrasCarregada = false;
    private TextToSpeech textToSpeech;

    public translateFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        vista = inflater.inflate(R.layout.fragment_translate, container, false);
        fromSpin = vista.findViewById(R.id.fromSpinner);
        toSpin = vista.findViewById(R.id.toSpinner);
        source = vista.findViewById(R.id.editSource);
        bt_traduzir = vista.findViewById(R.id.traduzir);
        traduzido = vista.findViewById(R.id.traduzido);
        audio = vista.findViewById(R.id.audio);
        reproduzirAudio = vista.findViewById(R.id.reproduzir);

        audio.setOnClickListener(v -> startRecording());

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Definir os adaptadores para os Spinners
        ArrayAdapter<String> adapterFrom = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapterTo = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);

        // Adicionar as opções de idioma aos adaptadores
        adapterFrom.add("Português");
        adapterFrom.add("Indígena");

        adapterTo.add("Indígena");
        adapterTo.add("Português");

        adapterFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpin.setAdapter(adapterFrom);
        toSpin.setAdapter(adapterTo);


        bt_traduzir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                traduzirPalavra();
            }
        });
        palavras();
        lerPalavrasDoBancoDeDados();

        textToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    Locale localeBR = new Locale("pt", "BR");
                    textToSpeech.setLanguage(localeBR);
                }
            }
        });
        reproduzirAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textoParaFalar = traduzido.getText().toString();
                textToSpeech.speak(textoParaFalar, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        return vista;
    }
    public void falarTexto(View view) {
        String frase = traduzido.getText().toString();

        if (!frase.isEmpty()) {
            textToSpeech.speak(frase, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void startRecording() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {

        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String transcribedText = result.get(0);
                source.setText(transcribedText);
            }
        }
    }

    private void traduzirPalavra() {
        String palavraOriginal = source.getText().toString();
        String idiomaOrigem = fromSpin.getSelectedItem().toString();
        String idiomaDestino = toSpin.getSelectedItem().toString();

        // Verificar se os idiomas de origem e destino são iguais
        if (idiomaOrigem.equals(idiomaDestino)) {
            traduzido.setText("Os idiomas de origem e destino devem ser diferentes");
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("dicionario");

        Query query;

        if (idiomaOrigem.equals("Português") && idiomaDestino.equals("Indígena")) {
            query = databaseRef.orderByChild("traducao").equalTo(palavraOriginal);
        } else if (idiomaOrigem.equals("Indígena") && idiomaDestino.equals("Português")) {
            query = databaseRef.orderByChild("palavraOriginal").equalTo(palavraOriginal);
        } else {
            // Caso de idioma não encontrado
            traduzido.setText("Tradução não encontrada");
            return;
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean traducaoEncontrada = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Palavra palavra = snapshot.getValue(Palavra.class);

                    if (idiomaOrigem.equals("Português") && idiomaDestino.equals("Indígena") && palavra.getTraducao().equalsIgnoreCase(palavraOriginal)) {
                        traduzido.setText(palavra.getPalavraOriginal());
                        traducaoEncontrada = true;
                        break;
                    } else if (idiomaOrigem.equals("Indígena") && idiomaDestino.equals("Português") && palavra.getPalavraOriginal().equalsIgnoreCase(palavraOriginal)) {
                        traduzido.setText(palavra.getTraducao());
                        traducaoEncontrada = true;
                        break;
                    }
                }

                if (!traducaoEncontrada) {
                    traduzido.setText("Tradução não encontrada");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                traduzido.setText("Erro ao acessar o banco de dados");
            }
        });

        // Remover o listener após a primeira consulta para evitar repetições
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Remover o listener após a primeira consulta
                query.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Tratar o cancelamento da consulta, se necessário
            }
        });
    }
    private String removeUpperCase(String input) {
        return input.replaceAll("[A-Z]", "").toLowerCase();
    }
    private void lerPalavrasDoBancoDeDados() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("dicionario");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaPalavrasFirebase = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Palavra palavra = snapshot.getValue(Palavra.class);
                    listaPalavrasFirebase.add(palavra);
                }

                // Verifique se a lista de palavras foi preenchida corretamente
                for (Palavra palavra : listaPalavrasFirebase) {
                    Log.d("Firebase", "Palavra: " + palavra.getPalavraOriginal() + ", Tradução: " + palavra.getTraducao());
                }

                // Chame o método traduzirPalavra() após carregar as palavras do banco de dados
                traduzirPalavra();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Erro ao ler o banco de dados");
            }
        });
    }

    private void palavras(){
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("dicionario");

        Palavra palavra1 = new Palavra("Ihot'ok", "Bom dia");
        databaseRef.child("palavra1").setValue(palavra1);

        Palavra palavra1_1 = new Palavra("ihot'ok", "bom dia");
        databaseRef.child("palavra1_1").setValue(palavra1_1);

        Palavra palavra2 = new Palavra("Heika'at", "Boa tarde");
        databaseRef.child("palavra2").setValue(palavra2);

        Palavra palavra2_2 = new Palavra("heika'at", "boa tarde");
        databaseRef.child("palavra2_2").setValue(palavra2_2);

        Palavra palavra3 = new Palavra("Wantym", "Boa noite");
        databaseRef.child("palavra3").setValue(palavra3);

        Palavra palavra3_3 = new Palavra("wantym", "boa noite");
        databaseRef.child("palavra3_3").setValue(palavra3_3);

        Palavra palavra4 = new Palavra("Meiko ira'yn aru", "Até mais");
        databaseRef.child("palavra4").setValue(palavra4);

        Palavra palavra4_4 = new Palavra("meiko ira'yn aru", "até mais");
        databaseRef.child("palavra4_4").setValue(palavra4_4);

        Palavra palavra5 = new Palavra("Mogki'ite ira'yn aru", "Até amanhã");
        databaseRef.child("palavra5").setValue(palavra5);

        Palavra palavra5_5 = new Palavra("mogki'ite ira'yn aru", "até amanhã");
        databaseRef.child("palavra5_5").setValue(palavra5_5);

        Palavra palavra6 = new Palavra("Hay", "Olá");
        databaseRef.child("palavra6").setValue(palavra6);

        Palavra palavra6_6 = new Palavra("hay", "olá");
        databaseRef.child("palavra6_6").setValue(palavra6_6);

        Palavra palavra7 = new Palavra("Waku sese", "Muito obrigado");
        databaseRef.child("palavra7").setValue(palavra7);

        Palavra palavra7_7 = new Palavra("waku sese", "muito obrigado");
        databaseRef.child("palavra7_7").setValue(palavra7_7);

        Palavra palavra8 = new Palavra("Meke arekosap", "Com licença");
        databaseRef.child("palavra8").setValue(palavra8);

        Palavra palavra8_8 = new Palavra("meke arekosap", "com licença");
        databaseRef.child("palavra8_8").setValue(palavra8_8);

        Palavra palavra8_88 = new Palavra("Mekepuo aru", "Com licença");
        databaseRef.child("palavra8_88").setValue(palavra8_88);

        Palavra palavra8_888 = new Palavra("mekepuo aru", "com licença");
        databaseRef.child("palavra8_888").setValue(palavra8_888);

        Palavra palavra9 = new Palavra("Yt kat hap hin'i", "De nada");
        databaseRef.child("palavra9").setValue(palavra9);

        Palavra palavra9_9 = new Palavra("yt kat hap hin'i", "de nada");
        databaseRef.child("palavra9_9").setValue(palavra9_9);

        Palavra palavra10 = new Palavra("Waku?", "Tudo bem?");
        databaseRef.child("palavra10").setValue(palavra10);

        Palavra palavra10_10 = new Palavra("waku?", "tudo bem?");
        databaseRef.child("palavra10_10").setValue(palavra10_10);

        Palavra palavra11 = new Palavra("Waku", "Bem");
        databaseRef.child("palavra11").setValue(palavra11);

        Palavra palavra11_11 = new Palavra("waku", "bem");
        databaseRef.child("palavra11_11").setValue(palavra11_11);

        Palavra palavra12 = new Palavra("Waku sese eriot", "Seja bem-vindo");
        databaseRef.child("palavra12").setValue(palavra12);

        Palavra palavra12_12 = new Palavra("waku sese eriot", "seja bem-vindo");
        databaseRef.child("palavra12_12").setValue(palavra12_12);

        Palavra palavra13 = new Palavra("Akag", "Cabeça");
        databaseRef.child("palavra13").setValue(palavra13);

        Palavra palavra13_13 = new Palavra("akag", "cabeça");
        databaseRef.child("palavra13_13").setValue(palavra13_13);

        Palavra palavra14 = new Palavra("Asap", "Cabelo");
        databaseRef.child("palavra14").setValue(palavra14);

        Palavra palavra14_14 = new Palavra("asap", "cabelo");
        databaseRef.child("palavra14_14").setValue(palavra14_14);

        Palavra palavra15 = new Palavra("Ape", "Costa");
        databaseRef.child("palavra15").setValue(palavra15);

        Palavra palavra15_15 = new Palavra("ape", "costa");
        databaseRef.child("palavra15_15").setValue(palavra15_15);

        Palavra palavra16 = new Palavra("Ampy", "Nariz");
        databaseRef.child("palavra16").setValue(palavra16);

        Palavra palavra16_16 = new Palavra("ampy", "nariz");
        databaseRef.child("palavra16_16").setValue(palavra16_16);

        Palavra palavra17 = new Palavra("Ahape", "Orelha");
        databaseRef.child("palavra17").setValue(palavra17);

        Palavra palavra17_17 = new Palavra("ahape", "orelha");
        databaseRef.child("palavra17_17").setValue(palavra17_17);

        Palavra palavra18 = new Palavra("Anti'ypy", "Ombro");
        databaseRef.child("palavra18").setValue(palavra18);

        Palavra palavra18_18 = new Palavra("anti'ypy", "ombro");
        databaseRef.child("palavra18_18").setValue(palavra18_18);

        Palavra palavra19 = new Palavra("Ahit", "Pênis");
        databaseRef.child("palavra19").setValue(palavra19);

        Palavra palavra19_19 = new Palavra("ahit", "pênis");
        databaseRef.child("palavra19_19").setValue(palavra19_19);

        Palavra palavra20 = new Palavra("Ikag", "Osso");
        databaseRef.child("palavra20").setValue(palavra20);

        Palavra palavra20_20 = new Palavra("ikag", "osso");
        databaseRef.child("palavra20_20").setValue(palavra20_20);

        Palavra palavra21 = new Palavra("Jaig", "Dente");
        databaseRef.child("palavra21").setValue(palavra21);

        Palavra palavra21_21 = new Palavra("jaig", "dente");
        databaseRef.child("palavra21_21").setValue(palavra21_21);

        Palavra palavra22 = new Palavra("Jun", "Fezes");
        databaseRef.child("palavra22").setValue(palavra22);

        Palavra palavra22_22 = new Palavra("jun", "fezes");
        databaseRef.child("palavra22_22").setValue(palavra22_22);

        Palavra palavra23 = new Palavra("Junmy'a", "Barriga");
        databaseRef.child("palavra23").setValue(palavra23);

        Palavra palavra23_23 = new Palavra("junmy'a", "barriga");
        databaseRef.child("palavra23_23").setValue(palavra23_23);

        Palavra palavra24 = new Palavra("Kag'oktu", "Perna");
        databaseRef.child("palavra24").setValue(palavra24);

        Palavra palavra24_24 = new Palavra("kag'oktu", "perna");
        databaseRef.child("palavra24_24").setValue(palavra24_24);

        Palavra palavra25 = new Palavra("Moti'a", "Peito");
        databaseRef.child("palavra25").setValue(palavra25);

        Palavra palavra25_25 = new Palavra("moti'a", "peito");
        databaseRef.child("palavra25_25").setValue(palavra25_25);

        Palavra palavra26 = new Palavra("Mu'uja", "Dedo");
        databaseRef.child("palavra26").setValue(palavra26);

        Palavra palavra26_26 = new Palavra("mu'uja", "dedo");
        databaseRef.child("palavra26_26").setValue(palavra26_26);

        Palavra palavra27 = new Palavra("My'a", "Fígado");
        databaseRef.child("palavra27").setValue(palavra27);

        Palavra palavra27_27 = new Palavra("my'a", "fígado");
        databaseRef.child("palavra27_27").setValue(palavra27_27);

        Palavra palavra28 = new Palavra("My'akag'a", "Joelho");
        databaseRef.child("palavra28").setValue(palavra28);

        Palavra palavra28_28 = new Palavra("my'akag'a", "joelho");
        databaseRef.child("palavra28_28").setValue(palavra28_28);

        Palavra palavra29 = new Palavra("Mo", "Mão");
        databaseRef.child("palavra29").setValue(palavra29);

        Palavra palavra29_29 = new Palavra("mo", "mão");
        databaseRef.child("palavra29_29").setValue(palavra29_29);

        Palavra palavra30 = new Palavra("My", "Pé");
        databaseRef.child("palavra30").setValue(palavra30);

        Palavra palavra30_30 = new Palavra("my", "pé");
        databaseRef.child("palavra30_30").setValue(palavra30_30);

        Palavra palavra31 = new Palavra("Myawa", "Sola do pé");
        databaseRef.child("palavra31").setValue(palavra31);

        Palavra palavra31_31 = new Palavra("myawa", "sola do pé");
        databaseRef.child("palavra31_31").setValue(palavra31_31);

        Palavra palavra32 = new Palavra("My'okpe", "Parte superior do pé");
        databaseRef.child("palavra32").setValue(palavra32);

        Palavra palavra32_32 = new Palavra("my'okpe", "parte superior do pé");
        databaseRef.child("palavra32_32").setValue(palavra32_32);

        Palavra palavra33 = new Palavra("Mopy'akag'a", "Cotovelo");
        databaseRef.child("palavra33").setValue(palavra33);

        Palavra palavra33_33 = new Palavra("mopy'akag'a", "cotovelo");
        databaseRef.child("palavra33_33").setValue(palavra33_33);

        Palavra palavra34 = new Palavra("My'ampát", "Articulação do joelho");
        databaseRef.child("palavra34").setValue(palavra34);

        Palavra palavra34_34 = new Palavra("my'ampát", "articulação do joelho");
        databaseRef.child("palavra34_34").setValue(palavra34_34);

        Palavra palavra35 = new Palavra("Myrum'a", "Umbigo");
        databaseRef.child("palavra35").setValue(palavra35);

        Palavra palavra35_35 = new Palavra("myrum'a", "umbigo");
        databaseRef.child("palavra35_35").setValue(palavra35_35);

        Palavra palavra36 = new Palavra("Nywa", "Queixo");
        databaseRef.child("palavra36").setValue(palavra36);

        Palavra palavra36_36 = new Palavra("nywa", "queixo");
        databaseRef.child("palavra36_36").setValue(palavra36_36);

        Palavra palavra37 = new Palavra("Oktu", "Coxa");
        databaseRef.child("palavra37").setValue(palavra37);

        Palavra palavra37_37 = new Palavra("oktu/uptu", "coxa");
        databaseRef.child("palavra37_37").setValue(palavra37_37);

        Palavra palavra37_3737 = new Palavra("Uptu", "Coxa");
        databaseRef.child("palavra37_3737").setValue(palavra37_3737);

        Palavra palavra37_373737 = new Palavra("uptu", "coxa");
        databaseRef.child("palavra37_373737").setValue(palavra37_373737);

        Palavra palavra38 = new Palavra("Okpy", "Nus");
        databaseRef.child("palavra38").setValue(palavra38);

        Palavra palavra38_38 = new Palavra("okpy", "nus");
        databaseRef.child("palavra38_38").setValue(palavra38_38);

        Palavra palavra39 = new Palavra("Sempe", "Beiço");
        databaseRef.child("palavra39").setValue(palavra39);

        Palavra palavra39_39 = new Palavra("sempe", "beiço");
        databaseRef.child("palavra39_39").setValue(palavra39_39);

        Palavra palavra40 = new Palavra("Setu'a", "Nádegas");
        databaseRef.child("palavra40").setValue(palavra40);

        Palavra palavra40_40 = new Palavra("setu'a", "nádegas");
        databaseRef.child("palavra40_40").setValue(palavra40_40);

        Palavra palavra41 = new Palavra("Seha", "Olho");
        databaseRef.child("palavra41").setValue(palavra41);

        Palavra palavra41_41 = new Palavra("seha", "olho");
        databaseRef.child("palavra41_41").setValue(palavra41_41);

        Palavra palavra42 = new Palavra("MÍt Pít Ekaria'i", "Anatomia do Corpo Humano");
        databaseRef.child("palavra42").setValue(palavra42);

        Palavra palavra43 = new Palavra("Sewa", "Rosto");
        databaseRef.child("palavra43").setValue(palavra43);

        Palavra palavra43_43 = new Palavra("sewa", "rosto");
        databaseRef.child("palavra43_43").setValue(palavra43_43);

        Palavra palavra44 = new Palavra("Segku", "Língua");
        databaseRef.child("palavra44").setValue(palavra44);

        Palavra palavra44_44 = new Palavra("segku", "língua");
        databaseRef.child("palavra44_44").setValue(palavra44_44);

        Palavra palavra45 = new Palavra("Si'ã", "Vagina");
        databaseRef.child("palavra45").setValue(palavra45);

        Palavra palavra45_45 = new Palavra("si'ã", "vagina");
        databaseRef.child("palavra45_45").setValue(palavra45_45);

        Palavra palavra46 = new Palavra("Wẽ", "Boca");
        databaseRef.child("palavra46").setValue(palavra46);

        Palavra palavra46_46 = new Palavra("wẽ", "boca");
        databaseRef.child("palavra46_46").setValue(palavra46_46);

        Palavra palavra47 = new Palavra("Wesap", "Bogode");
        databaseRef.child("palavra47").setValue(palavra47);

        Palavra palavra47_47 = new Palavra("wesap", "bogode");
        databaseRef.child("palavra47_47").setValue(palavra47_47);

        Palavra palavra48 = new Palavra("Wegkot'a", "Bochecha");
        databaseRef.child("palavra48").setValue(palavra48);

        Palavra palavra48_48 = new Palavra("wegkot'a", "bochecha");
        databaseRef.child("palavra48_48").setValue(palavra48_48);

        Palavra palavra49 = new Palavra("Watu'a", "Testa");
        databaseRef.child("palavra49").setValue(palavra49);

        Palavra palavra49_49 = new Palavra("watu'a", "testa");
        databaseRef.child("palavra49_49").setValue(palavra49_49);

        Palavra palavra50 = new Palavra("Yke", "Braço");
        databaseRef.child("palavra50").setValue(palavra50);

        Palavra palavra50_50 = new Palavra("yke", "braço");
        databaseRef.child("palavra50_50").setValue(palavra50_50);

        Palavra palavra51 = new Palavra("Akuri", "Cutia");
        databaseRef.child("palavra51").setValue(palavra51);

        Palavra palavra51_51 = new Palavra("akuri", "cutia");
        databaseRef.child("palavra51_51").setValue(palavra51_51);

        Palavra palavra52 = new Palavra("Awahuru", "Cachorro do mato");
        databaseRef.child("palavra52").setValue(palavra52);

        Palavra palavra52_52 = new Palavra("awahuru", "cachorro do mato");
        databaseRef.child("palavra52_52").setValue(palavra52_52);

        Palavra palavra53 = new Palavra("Awyky", "Guariba");
        databaseRef.child("palavra53").setValue(palavra53);

        Palavra palavra53_53 = new Palavra("awyky", "guariba");
        databaseRef.child("palavra53_53").setValue(palavra53_53);

        Palavra palavra54 = new Palavra("Awyato", "Onça");
        databaseRef.child("palavra54").setValue(palavra54);

        Palavra palavra54_54 = new Palavra("awyato", "onça");
        databaseRef.child("palavra54_54").setValue(palavra54_54);

        Palavra palavra55 = new Palavra("Ariukere", "Preguiça");
        databaseRef.child("palavra55").setValue(palavra55);

        Palavra palavra55_55 = new Palavra("ariukere", "preguiça");
        databaseRef.child("palavra55_55").setValue(palavra55_55);

        Palavra palavra56 = new Palavra("Hapiri", "Rato");
        databaseRef.child("palavra56").setValue(palavra56);

        Palavra palavra56_56 = new Palavra("hapiri", "rato");
        databaseRef.child("palavra56_56").setValue(palavra56_56);

        Palavra palavra57 = new Palavra("Hamaut", "Porco");
        databaseRef.child("palavra57").setValue(palavra57);

        Palavra palavra57_57 = new Palavra("hamaut", "porco");
        databaseRef.child("palavra57_57").setValue(palavra57_57);

        Palavra palavra58 = new Palavra("Himpa", "Tamanduá bandeira");
        databaseRef.child("palavra58").setValue(palavra58);

        Palavra palavra58_58 = new Palavra("himpa", "tamanduá bandeira");
        databaseRef.child("palavra58_58").setValue(palavra58_58);

        Palavra palavra59 = new Palavra("Hami'ῖ", "Xauim");
        databaseRef.child("palavra59").setValue(palavra59);

        Palavra palavra59_59 = new Palavra("hami'ῖ", "xauim");
        databaseRef.child("palavra59_59").setValue(palavra59_59);

        Palavra palavra60 = new Palavra("Hanu'án", "Macaco prego");
        databaseRef.child("palavra60").setValue(palavra60);

        Palavra palavra60_60 = new Palavra("hanu'án", "macaco prego");
        databaseRef.child("palavra60_60").setValue(palavra60_60);

        Palavra palavra61 = new Palavra("Iakare", "Jacaré");
        databaseRef.child("palavra61").setValue(palavra61);

        Palavra palavra61_61 = new Palavra("iakare", "jacaré");
        databaseRef.child("palavra61_61").setValue(palavra61_61);

        Palavra palavra62 = new Palavra("Moi", "Cobra");
        databaseRef.child("palavra62").setValue(palavra62);

        Palavra palavra62_62 = new Palavra("moi", "cobra");
        databaseRef.child("palavra62_62").setValue(palavra62_62);

        Palavra palavra63 = new Palavra("Pay", "Paca");
        databaseRef.child("palavra63").setValue(palavra63);

        Palavra palavra63_63 = new Palavra("pay", "paca");
        databaseRef.child("palavra63_63").setValue(palavra63_63);

        Palavra palavra64 = new Palavra("Sahu", "Tatu");
        databaseRef.child("palavra64").setValue(palavra64);

        Palavra palavra64_64 = new Palavra("sahu", "tatu");
        databaseRef.child("palavra64_64").setValue(palavra64_64);

        Palavra palavra65 = new Palavra("Wewato", "Anta");
        databaseRef.child("palavra65").setValue(palavra65);

        Palavra palavra65_65 = new Palavra("wewato", "anta");
        databaseRef.child("palavra65_65").setValue(palavra65_65);

        Palavra palavra66 = new Palavra("Wawori", "Jabuti");
        databaseRef.child("palavra66").setValue(palavra66);

        Palavra palavra66_66 = new Palavra("wawori", "jabuti");
        databaseRef.child("palavra66_66").setValue(palavra66_66);

        Palavra palavra67 = new Palavra("Wakiu'i wato", "Macaco velho");
        databaseRef.child("palavra67").setValue(palavra67);

        Palavra palavra67_67 = new Palavra("wakiu'i wato", "macaco velho");
        databaseRef.child("palavra67_67").setValue(palavra67_67);

        Palavra palavra68 = new Palavra("Ytý wato", "Veado grande");
        databaseRef.child("palavra68").setValue(palavra68);

        Palavra palavra68_68 = new Palavra("ytý wato", "veado grande");
        databaseRef.child("palavra68_68").setValue(palavra68_68);

        Palavra palavra69 = new Palavra("Ytý hít", "Veado pequeno");
        databaseRef.child("palavra69").setValue(palavra69);

        Palavra palavra69_69 = new Palavra("ytý hít", "veado pequeno");
        databaseRef.child("palavra69_69").setValue(palavra69_69);

        Palavra palavra70 = new Palavra("Ahut", "Papagaio");
        databaseRef.child("palavra70").setValue(palavra70);

        Palavra palavra70_70 = new Palavra("ahut", "papagaio");
        databaseRef.child("palavra70_70").setValue(palavra70_70);

        Palavra palavra71 = new Palavra("Hyt'i", "Beija-flor");
        databaseRef.child("palavra71").setValue(palavra71);

        Palavra palavra71_71 = new Palavra("hyt'i", "beija-flor");
        databaseRef.child("palavra71_71").setValue(palavra71_71);

        Palavra palavra72 = new Palavra("Hanún", "Arara");
        databaseRef.child("palavra72").setValue(palavra72);

        Palavra palavra72_72 = new Palavra("hanún", "arara");
        databaseRef.child("palavra72_72").setValue(palavra72_72);

        Palavra palavra73 = new Palavra("Hywi wato", "Gavião real");
        databaseRef.child("palavra73").setValue(palavra73);

        Palavra palavra73_73 = new Palavra("hywi wato", "gavião real");
        databaseRef.child("palavra73_73").setValue(palavra73_73);

        Palavra palavra74 = new Palavra("Jugkan", "Tucano");
        databaseRef.child("palavra74").setValue(palavra74);

        Palavra palavra74_74 = new Palavra("jugkan", "tucano");
        databaseRef.child("palavra74_74").setValue(palavra74_74);

        Palavra palavra75 = new Palavra("Myju", "Jacú");
        databaseRef.child("palavra75").setValue(palavra75);

        Palavra palavra75_75 = new Palavra("myju", "jacú");
        databaseRef.child("palavra75_75").setValue(palavra75_75);

        Palavra palavra76 = new Palavra("Pykasu", "Pombo");
        databaseRef.child("palavra76").setValue(palavra76);

        Palavra palavra76_76 = new Palavra("pykasu", "pombo");
        databaseRef.child("palavra76_76").setValue(palavra76_76);

        Palavra palavra77 = new Palavra("Pirikítu", "Periquito");
        databaseRef.child("palavra77").setValue(palavra77);

        Palavra palavra77_77 = new Palavra("pirikítu", "periquito");
        databaseRef.child("palavra77_77").setValue(palavra77_77);

        Palavra palavra78 = new Palavra("Samã", "Pica-pau");
        databaseRef.child("palavra78").setValue(palavra78);

        Palavra palavra78_78 = new Palavra("samã", "pica-pau");
        databaseRef.child("palavra78_78").setValue(palavra78_78);

        Palavra palavra79 = new Palavra("Weita", "Pássaro");
        databaseRef.child("palavra79").setValue(palavra79);

        Palavra palavra79_79 = new Palavra("weita", "pássaro");
        databaseRef.child("palavra79_79").setValue(palavra79_79);

        Palavra palavra80 = new Palavra("Urukut", "Coruja");
        databaseRef.child("palavra80").setValue(palavra80);

        Palavra palavra80_80 = new Palavra("urukut", "coruja");
        databaseRef.child("palavra80_80").setValue(palavra80_80);

        Palavra palavra81 = new Palavra("Úre", "Jacamim");
        databaseRef.child("palavra81").setValue(palavra81);

        Palavra palavra81_81 = new Palavra("úre", "jacamim");
        databaseRef.child("palavra81_81").setValue(palavra81_81);

        Palavra palavra82 = new Palavra("Urit'i", "Inambú");
        databaseRef.child("palavra82").setValue(palavra82);

        Palavra palavra82_82 = new Palavra("urit'i", "inambú");
        databaseRef.child("palavra82_82").setValue(palavra82_82);

        Palavra palavra83 = new Palavra("Weita hít", "Passarinho");
        databaseRef.child("palavra83").setValue(palavra83);

        Palavra palavra83_83 = new Palavra("weita hít", "passarinho");
        databaseRef.child("palavra83_83").setValue(palavra83_83);

        Palavra palavra84 = new Palavra("Wiawu", "Mutum");
        databaseRef.child("palavra84").setValue(palavra84);

        Palavra palavra84_84 = new Palavra("wiawu", "mutum");
        databaseRef.child("palavra84_84").setValue(palavra84_84);

        Palavra palavra85 = new Palavra("Awi'a", "Abelha");
        databaseRef.child("palavra85").setValue(palavra85);

        Palavra palavra85_85 = new Palavra("awi'a", "abelha");
        databaseRef.child("palavra85_85").setValue(palavra85_85);

        Palavra palavra86 = new Palavra("Ape'i", "Barata Gap");
        databaseRef.child("palavra86").setValue(palavra86);

        Palavra palavra86_86 = new Palavra("ape'i", "barata gap");
        databaseRef.child("palavra86_86").setValue(palavra86_86);

        Palavra palavra86_8686 = new Palavra("Arawe", "Barata Gap");
        databaseRef.child("palavra86_8686").setValue(palavra86_8686);

        Palavra palavra86_868686 = new Palavra("arawe", "barata Gap");
        databaseRef.child("palavra86_868686").setValue(palavra86_868686);

        Palavra palavra87 = new Palavra("Caba", "Inseto que voa");
        databaseRef.child("palavra87").setValue(palavra87);

        Palavra palavra87_87 = new Palavra("caba", "inseto que voa");
        databaseRef.child("palavra87_87").setValue(palavra87_87);

        Palavra palavra88 = new Palavra("Gyp", "Piolho");
        databaseRef.child("palavra88").setValue(palavra88);

        Palavra palavra88_88 = new Palavra("gyp", "piolho");
        databaseRef.child("palavra88_88").setValue(palavra88_88);

        Palavra palavra89 = new Palavra("Jug", "Pulga");
        databaseRef.child("palavra89").setValue(palavra89);

        Palavra palavra89_89 = new Palavra("jug", "pulga");
        databaseRef.child("palavra89_89").setValue(palavra89_89);

        Palavra palavra90 = new Palavra("Kiã", "Aranha");
        databaseRef.child("palavra90").setValue(palavra90);

        Palavra palavra90_90 = new Palavra("kiã", "aranha");
        databaseRef.child("palavra90_90").setValue(palavra90_90);

        Palavra palavra91 = new Palavra("Kíwa", "Tapecuim");
        databaseRef.child("palavra91").setValue(palavra91);

        Palavra palavra91_91 = new Palavra("kíwa", "tapecuim");
        databaseRef.child("palavra91_91").setValue(palavra91_91);

        Palavra palavra92 = new Palavra("Karawót", "Cigarra");
        databaseRef.child("palavra92").setValue(palavra92);

        Palavra palavra92_92 = new Palavra("karawót", "cigarra");
        databaseRef.child("palavra92_92").setValue(palavra92_92);

        Palavra palavra93 = new Palavra("Mantéru", "Vaga-lume");
        databaseRef.child("palavra93").setValue(palavra93);

        Palavra palavra93_93 = new Palavra("mantéru", "vaga-lume");
        databaseRef.child("palavra93_93").setValue(palavra93_93);

        Palavra palavra94 = new Palavra("Morope'i", "Borboleta");
        databaseRef.child("palavra94").setValue(palavra94);

        Palavra palavra94_94 = new Palavra("morope'i", "borboleta");
        databaseRef.child("palavra94_94").setValue(palavra94_94);

        Palavra palavra95 = new Palavra("Mokag", "Louva-a-deus");
        databaseRef.child("palavra95").setValue(palavra95);

        Palavra palavra95_95 = new Palavra("mokag", "louva-a-deus");
        databaseRef.child("palavra95_95").setValue(palavra95_95);

        Palavra palavra96 = new Palavra("Pohit", "Gafanhoto");
        databaseRef.child("palavra96").setValue(palavra96);

        Palavra palavra96_96 = new Palavra("pohit", "gafanhoto");
        databaseRef.child("palavra96_96").setValue(palavra96_96);

        Palavra palavra97 = new Palavra("Sapót", "Escorpião");
        databaseRef.child("palavra97").setValue(palavra97);

        Palavra palavra97_97 = new Palavra("sapót", "escorpião");
        databaseRef.child("palavra97_97").setValue(palavra97_97);

        Palavra palavra98 = new Palavra("Sári", "Formiga");
        databaseRef.child("palavra98").setValue(palavra98);

        Palavra palavra98_98 = new Palavra("sári", "formiga");
        databaseRef.child("palavra98_98").setValue(palavra98_98);

        Palavra palavra99 = new Palavra("Sahai", "Saúva-taia");
        databaseRef.child("palavra99").setValue(palavra99);

        Palavra palavra99_99 = new Palavra("sahai", "saúva-taia");
        databaseRef.child("palavra99_99").setValue(palavra99_99);

        Palavra palavra100 = new Palavra("Úwi", "Minhoca");
        databaseRef.child("palavra100").setValue(palavra100);

        Palavra palavra100_100 = new Palavra("úwi", "minhoca");
        databaseRef.child("palavra100_100").setValue(palavra100_100);

        Palavra palavra101 = new Palavra("Ut", "Lagarto");
        databaseRef.child("palavra101").setValue(palavra101);

        Palavra palavra101_101 = new Palavra("ut", "lagarto");
        databaseRef.child("palavra101_101").setValue(palavra101_101);

        Palavra palavra102 = new Palavra("Upi'u", "Maruim");
        databaseRef.child("palavra102").setValue(palavra102);

        Palavra palavra102_102 = new Palavra("upi'u", "maruim");
        databaseRef.child("palavra102_102").setValue(palavra102_102);

        Palavra palavra103 = new Palavra("We'ehog", "Maniwara");
        databaseRef.child("palavra103").setValue(palavra103);

        Palavra palavra103_103 = new Palavra("we'ehog", "maniwara");
        databaseRef.child("palavra103_103").setValue(palavra103_103);

        Palavra palavra104 = new Palavra("Wanti'u", "Carapanã");
        databaseRef.child("palavra104").setValue(palavra104);

        Palavra palavra104_104 = new Palavra("wanti'u", "carapanã");
        databaseRef.child("palavra104_104").setValue(palavra104_104);

        Palavra palavra105 = new Palavra("Watyama", "Tucandeira");
        databaseRef.child("palavra105").setValue(palavra105);

        Palavra palavra105_105 = new Palavra("watyama", "tucandeira");
        databaseRef.child("palavra105_105").setValue(palavra105_105);

        Palavra palavra106 = new Palavra("Wáru'i", "Mucuim");
        databaseRef.child("palavra106").setValue(palavra106);

        Palavra palavra106_106 = new Palavra("wáru'i", "mucuim");
        databaseRef.child("palavra106_106").setValue(palavra106_106);

        Palavra palavra107 = new Palavra("Aware", "Cachorro");
        databaseRef.child("palavra107").setValue(palavra107);

        Palavra palavra107_107 = new Palavra("aware", "cachorro");
        databaseRef.child("palavra107_107").setValue(palavra107_107);

        Palavra palavra108 = new Palavra("Hamaut asuwai", "Porco suíno");
        databaseRef.child("palavra108").setValue(palavra108);

        Palavra palavra108_108 = new Palavra("hamaut asuwai", "porco suíno");
        databaseRef.child("palavra108_108").setValue(palavra108_108);

        Palavra palavra109 = new Palavra("Pisana", "Gato");
        databaseRef.child("palavra109").setValue(palavra109);

        Palavra palavra109_109 = new Palavra("pisana", "gato");
        databaseRef.child("palavra109_109").setValue(palavra109_109);

        Palavra palavra110 = new Palavra("Piku-piku", "Pinto");
        databaseRef.child("palavra110").setValue(palavra110);

        Palavra palavra110_110 = new Palavra("piku-piku", "pinto");
        databaseRef.child("palavra110_110").setValue(palavra110_110);

        Palavra palavra111 = new Palavra("Waipaka wáry'i", "Galinha");
        databaseRef.child("palavra111").setValue(palavra111);

        Palavra palavra111_111 = new Palavra("waipaka wáry'i", "galinha");
        databaseRef.child("palavra111_111").setValue(palavra111_111);

        Palavra palavra112 = new Palavra("Waipaka pa'aiat", "Galo");
        databaseRef.child("palavra112").setValue(palavra112);

        Palavra palavra112_112 = new Palavra("waipaka pa'aiat", "galo");
        databaseRef.child("palavra112_112").setValue(palavra112_112);

        Palavra palavra113 = new Palavra("Wewato ahup", "Boi");
        databaseRef.child("palavra113").setValue(palavra113);

        Palavra palavra113_113 = new Palavra("wewato ahup", "boi");
        databaseRef.child("palavra113_113").setValue(palavra113_113);

        Palavra palavra114 = new Palavra("Ypéka", "Pato");
        databaseRef.child("palavra114").setValue(palavra114);

        Palavra palavra114_114 = new Palavra("ypéka", "pato");
        databaseRef.child("palavra114_114").setValue(palavra114_114);

        Palavra palavra115 = new Palavra("Ase'i", "Vovô");
        databaseRef.child("palavra115").setValue(palavra115);

        Palavra palavra115_115 = new Palavra("ase'i", "vovô");
        databaseRef.child("palavra115_115").setValue(palavra115_115);

        Palavra palavra116 = new Palavra("Api'i", "Tia");
        databaseRef.child("palavra116").setValue(palavra116);

        Palavra palavra116_116 = new Palavra("api'i", "tia");
        databaseRef.child("palavra116_116").setValue(palavra116_116);

        Palavra palavra117 = new Palavra("Hamῦ", "Tio");
        databaseRef.child("palavra117").setValue(palavra117);

        Palavra palavra117_117 = new Palavra("hamῦ", "tio");
        databaseRef.child("palavra117_117").setValue(palavra117_117);

        Palavra palavra118 = new Palavra("Hary", "Vovó");
        databaseRef.child("palavra118").setValue(palavra118);

        Palavra palavra118_118 = new Palavra("hary", "vovó");
        databaseRef.child("palavra118_118").setValue(palavra118_118);

        Palavra palavra119 = new Palavra("Hemiariru", "Neto");
        databaseRef.child("palavra119").setValue(palavra119);

        Palavra palavra119_119 = new Palavra("hemiariru", "neto");
        databaseRef.child("palavra119_119").setValue(palavra119_119);

        Palavra palavra120 = new Palavra("Hemiarira", "Neta");
        databaseRef.child("palavra120").setValue(palavra120);

        Palavra palavra120_120 = new Palavra("hemiarira", "neta");
        databaseRef.child("palavra120_120").setValue(palavra120_120);

        Palavra palavra121 = new Palavra("Isaig", "Menino");
        databaseRef.child("palavra121").setValue(palavra121);

        Palavra palavra121_121 = new Palavra("isaig", "menino");
        databaseRef.child("palavra121_121").setValue(palavra121_121);

        Palavra palavra122 = new Palavra("Hirokat", "Criança");
        databaseRef.child("palavra122").setValue(palavra122);

        Palavra palavra122_122 = new Palavra("hirokat", "criança");
        databaseRef.child("palavra122_122").setValue(palavra122_122);

        Palavra palavra123 = new Palavra("Haryporia", "Mulher");
        databaseRef.child("palavra123").setValue(palavra123);

        Palavra palavra123_123 = new Palavra("haryporia", "mulher");
        databaseRef.child("palavra123_123").setValue(palavra123_123);

        Palavra palavra124 = new Palavra("Iywyt", "Irmão mais novo");
        databaseRef.child("palavra124").setValue(palavra124);

        Palavra palavra124_124 = new Palavra("iywyt", "irmão mais novo");
        databaseRef.child("palavra124_124").setValue(palavra124_124);

        Palavra palavra125 = new Palavra("Iyke'et", "Irmão mais velho");
        databaseRef.child("palavra125").setValue(palavra125);

        Palavra palavra125_125 = new Palavra("iyke'et", "irmão mais velho");
        databaseRef.child("palavra125_125").setValue(palavra125_125);

        Palavra palavra126 = new Palavra("Ikypy'yt", "Irmã mais nova");
        databaseRef.child("palavra126").setValue(palavra126);

        Palavra palavra126_126 = new Palavra("ikypy'yt", "irmã mais nova");
        databaseRef.child("palavra126_126").setValue(palavra126_126);

        Palavra palavra127 = new Palavra("Iki'it", "Irmã mais velha");
        databaseRef.child("palavra127").setValue(palavra127);

        Palavra palavra127_127 = new Palavra("iki'it", "irmã mais velha");
        databaseRef.child("palavra127_127").setValue(palavra127_127);

        Palavra palavra128 = new Palavra("Ihaignia", "Homem");
        databaseRef.child("palavra128").setValue(palavra128);

        Palavra palavra128_128 = new Palavra("ihaignia", "homem");
        databaseRef.child("palavra128_128").setValue(palavra128_128);

        Palavra palavra129 = new Palavra("Kurum hít", "Menino");
        databaseRef.child("palavra129").setValue(palavra129);

        Palavra palavra129_129 = new Palavra("kurum hít", "menino");
        databaseRef.child("palavra129_129").setValue(palavra129_129);

        Palavra palavra130 = new Palavra("Kurum hín", "Menino");
        databaseRef.child("palavra130").setValue(palavra130);

        Palavra palavra130_130 = new Palavra("kurum hín", "menino");
        databaseRef.child("palavra130_130").setValue(palavra130_130);

        Palavra palavra131 = new Palavra("Kurum iwasu", "Moço");
        databaseRef.child("palavra131").setValue(palavra131);

        Palavra palavra131_131 = new Palavra("kurum iwasu", "moço");
        databaseRef.child("palavra131_131").setValue(palavra131_131);

        Palavra palavra132 = new Palavra("Makuptia", "Moça");
        databaseRef.child("palavra132").setValue(palavra132);

        Palavra palavra132_132 = new Palavra("makuptia", "moça");
        databaseRef.child("palavra132_132").setValue(palavra132_132);

        Palavra palavra133 = new Palavra("Mimῖ", "Mano");
        databaseRef.child("palavra133").setValue(palavra133);

        Palavra palavra133_133 = new Palavra("mimῖ", "mano");
        databaseRef.child("palavra133_133").setValue(palavra133_133);

        Palavra palavra134 = new Palavra("Mempyt", "Filho");
        databaseRef.child("palavra134").setValue(palavra134);

        Palavra palavra134_134 = new Palavra("mempyt", "filho");
        databaseRef.child("palavra134_134").setValue(palavra134_134);

        Palavra palavra135 = new Palavra("Ny", "Mãe");
        databaseRef.child("palavra135").setValue(palavra135);

        Palavra palavra135_135 = new Palavra("ny", "mãe");
        databaseRef.child("palavra135_135").setValue(palavra135_135);

        Palavra palavra136 = new Palavra("Ny hít", "Tia");
        databaseRef.child("palavra136").setValue(palavra136);

        Palavra palavra136_136 = new Palavra("ny hít", "tia");
        databaseRef.child("palavra136_136").setValue(palavra136_136);

        Palavra palavra137 = new Palavra("Ny tag", "Tia");
        databaseRef.child("palavra137").setValue(palavra137);

        Palavra palavra137_137 = new Palavra("ny tag", "tia");
        databaseRef.child("palavra137_137").setValue(palavra137_137);

        Palavra palavra138 = new Palavra("Pi'ã", "Mulher");
        databaseRef.child("palavra138").setValue(palavra138);

        Palavra palavra138_138 = new Palavra("pi'ã", "mulher");
        databaseRef.child("palavra138_138").setValue(palavra138_138);

        Palavra palavra139 = new Palavra("Pi'ã hít", "Menina");
        databaseRef.child("palavra139").setValue(palavra139);

        Palavra palavra139_139 = new Palavra("pi'ã hít", "menina");
        databaseRef.child("palavra139_139").setValue(palavra139_139);

        Palavra palavra140 = new Palavra("Uhehary'i", "Minha mulher");
        databaseRef.child("palavra140").setValue(palavra140);

        Palavra palavra140_140 = new Palavra("uhehary'i", "minha mulher");
        databaseRef.child("palavra140_140").setValue(palavra140_140);

        Palavra palavra141 = new Palavra("Uhe'aito", "Meu marido");
        databaseRef.child("palavra141").setValue(palavra141);

        Palavra palavra141_141 = new Palavra("uhe'aito", "meu marido");
        databaseRef.child("palavra141_141").setValue(palavra141_141);

        Palavra palavra142 = new Palavra("Uhýt", "Mano");
        databaseRef.child("palavra142").setValue(palavra142);

        Palavra palavra142_142 = new Palavra("uhýt", "mano");
        databaseRef.child("palavra142_142").setValue(palavra142_142);

        Palavra palavra143 = new Palavra("Uikywyt", "Irmão");
        databaseRef.child("palavra143").setValue(palavra143);

        Palavra palavra143_143 = new Palavra("uikywyt", "irmão");
        databaseRef.child("palavra143_143").setValue(palavra143_143);

        Palavra palavra144 = new Palavra("Ywót", "Pai");
        databaseRef.child("palavra144").setValue(palavra144);

        Palavra palavra144_144 = new Palavra("ywót", "pai");
        databaseRef.child("palavra144_144").setValue(palavra144_144);

        Palavra palavra145 = new Palavra("Ywót hít", "Tio");
        databaseRef.child("palavra145").setValue(palavra145);

        Palavra palavra145_145 = new Palavra("ywót hít", "tio");
        databaseRef.child("palavra145_145").setValue(palavra145_145);

        Palavra palavra146 = new Palavra("Ywót tag", "Tio");
        databaseRef.child("palavra146").setValue(palavra146);

        Palavra palavra146_146 = new Palavra("ywót tag", "tio");
        databaseRef.child("palavra146_146").setValue(palavra146_146);

        Palavra palavra147 = new Palavra("Hún", "Preto");
        databaseRef.child("palavra147").setValue(palavra147);

        Palavra palavra147_147 = new Palavra("hún", "preto");
        databaseRef.child("palavra147_147").setValue(palavra147_147);

        Palavra palavra148 = new Palavra("Hún'ok", "Cinza");
        databaseRef.child("palavra148").setValue(palavra148);

        Palavra palavra148_148 = new Palavra("hún'ok", "cinza");
        databaseRef.child("palavra148_148").setValue(palavra148_148);

        Palavra palavra149 = new Palavra("Ihup'ok", "Marrom");
        databaseRef.child("palavra149").setValue(palavra149);

        Palavra palavra149_149 = new Palavra("ihup'ok", "marrom");
        databaseRef.child("palavra149_149").setValue(palavra149_149);

        Palavra palavra150 = new Palavra("Ihup", "Vermelho");
        databaseRef.child("palavra150").setValue(palavra150);

        Palavra palavra150_150 = new Palavra("ihup", "vermelho");
        databaseRef.child("palavra150_150").setValue(palavra150_150);

        Palavra palavra151 = new Palavra("Ikytsig", "Branco");
        databaseRef.child("palavra151").setValue(palavra151);

        Palavra palavra151_151 = new Palavra("ikytsig", "branco");
        databaseRef.child("palavra151_151").setValue(palavra151_151);

        Palavra palavra152 = new Palavra("Ika'ay", "Amarelo");
        databaseRef.child("palavra152").setValue(palavra152);

        Palavra palavra152_152 = new Palavra("ika'ay", "amarelo");
        databaseRef.child("palavra152_152").setValue(palavra152_152);

        Palavra palavra153 = new Palavra("Ihyrýp", "Azul");
        databaseRef.child("palavra153").setValue(palavra153);

        Palavra palavra153_153 = new Palavra("ihyrýp", "azul");
        databaseRef.child("palavra153_153").setValue(palavra153_153);

        Palavra palavra154 = new Palavra("Ihyrýp ga'apy", "Verde");
        databaseRef.child("palavra154").setValue(palavra154);

        Palavra palavra154_154 = new Palavra("ihyrýp ga'apy", "verde");
        databaseRef.child("palavra154_154").setValue(palavra154_154);

        Palavra palavra155 = new Palavra("Apeman", "Feixe");
        databaseRef.child("palavra155").setValue(palavra155);

        Palavra palavra155_155 = new Palavra("apeman", "feixe");
        databaseRef.child("palavra155_155").setValue(palavra155_155);

        Palavra palavra156 = new Palavra("Aria", "Fogo");
        databaseRef.child("palavra156").setValue(palavra156);

        Palavra palavra156_156 = new Palavra("aria", "fogo");
        databaseRef.child("palavra156_156").setValue(palavra156_156);

        Palavra palavra157 = new Palavra("Apukuita", "Remo");
        databaseRef.child("palavra157").setValue(palavra157);

        Palavra palavra157_157 = new Palavra("apukuita", "remo");
        databaseRef.child("palavra157_157").setValue(palavra157_157);

        Palavra palavra158 = new Palavra("Apukuite", "Remo");
        databaseRef.child("palavra158").setValue(palavra158);

        Palavra palavra158_158 = new Palavra("apukuite", "remo");
        databaseRef.child("palavra158_158").setValue(palavra158_158);

        Palavra palavra159 = new Palavra("Awyhap", "Machado");
        databaseRef.child("palavra159").setValue(palavra159);

        Palavra palavra159_159 = new Palavra("awyhap", "machado");
        databaseRef.child("palavra159_159").setValue(palavra159_159);

        Palavra palavra160 = new Palavra("Awyhap", "Machado");
        databaseRef.child("palavra160").setValue(palavra160);

        Palavra palavra160_160 = new Palavra("awyhap", "machado");
        databaseRef.child("palavra160_160").setValue(palavra160_160);

        Palavra palavra161 = new Palavra("Go", "Roça");
        databaseRef.child("palavra161").setValue(palavra161);

        Palavra palavra161_161 = new Palavra("go", "roça");
        databaseRef.child("palavra161_161").setValue(palavra161_161);

        Palavra palavra162 = new Palavra("Hiwáre", "Pau de chuva");
        databaseRef.child("palavra162").setValue(palavra162);

        Palavra palavra162_162 = new Palavra("hiwáre", "pau de chuva");
        databaseRef.child("palavra162_162").setValue(palavra162_162);

        Palavra palavra163 = new Palavra("Hairu", "Dança");
        databaseRef.child("palavra163").setValue(palavra163);

        Palavra palavra163_163 = new Palavra("hairu", "dança");
        databaseRef.child("palavra163_163").setValue(palavra163_163);

        Palavra palavra164 = new Palavra("Já'ampe", "Chocalho");
        databaseRef.child("palavra164").setValue(palavra164);

        Palavra palavra164_164 = new Palavra("já'ampe", "chocalho");
        databaseRef.child("palavra164_164").setValue(palavra164_164);

        Palavra palavra165 = new Palavra("Kuiru'a", "Jamarú");
        databaseRef.child("palavra165").setValue(palavra165);

        Palavra palavra165_165 = new Palavra("kuiru'a", "jamarú");
        databaseRef.child("palavra165_165").setValue(palavra165_165);

        Palavra palavra166 = new Palavra("Kui'a", "Cuia");
        databaseRef.child("palavra166").setValue(palavra166);

        Palavra palavra166_166 = new Palavra("kui'a", "cuia");
        databaseRef.child("palavra166_166").setValue(palavra166_166);

        Palavra palavra167 = new Palavra("Kyse", "Faca");
        databaseRef.child("palavra167").setValue(palavra167);

        Palavra palavra167_167 = new Palavra("kyse", "faca");
        databaseRef.child("palavra167_167").setValue(palavra167_167);

        Palavra palavra168 = new Palavra("Kyse'yp", "Terçado");
        databaseRef.child("palavra168").setValue(palavra168);

        Palavra palavra168_168 = new Palavra("kyse'yp", "terçado");
        databaseRef.child("palavra168_168").setValue(palavra168_168);

        Palavra palavra169 = new Palavra("Kamunti", "Pote");
        databaseRef.child("palavra169").setValue(palavra169);

        Palavra palavra169_169 = new Palavra("kamunti", "pote");
        databaseRef.child("palavra169_169").setValue(palavra169_169);

        Palavra palavra170 = new Palavra("Kuriwu", "Jamaxin");
        databaseRef.child("palavra170").setValue(palavra170);

        Palavra palavra170_170 = new Palavra("kuriwu", "jamaxin");
        databaseRef.child("palavra170_170").setValue(palavra170_170);

        Palavra palavra171 = new Palavra("Móhoro", "Tipiti");
        databaseRef.child("palavra171").setValue(palavra171);

        Palavra palavra171_171 = new Palavra("móhoro", "tipiti");
        databaseRef.child("palavra171_171").setValue(palavra171_171);

        Palavra palavra172 = new Palavra("Musé", "Pimenta");
        databaseRef.child("palavra172").setValue(palavra172);

        Palavra palavra172_172 = new Palavra("musé", "pimenta");
        databaseRef.child("palavra172_172").setValue(palavra172_172);

        Palavra palavra173 = new Palavra("Man", "Beijú");
        databaseRef.child("palavra173").setValue(palavra173);

        Palavra palavra173_173 = new Palavra("man", "beijú");
        databaseRef.child("palavra173_173").setValue(palavra173_173);

        Palavra palavra174 = new Palavra("Manῖ", "Mandioca");
        databaseRef.child("palavra174").setValue(palavra174);

        Palavra palavra174_174 = new Palavra("manῖ", "mandioca");
        databaseRef.child("palavra174_174").setValue(palavra174_174);

        Palavra palavra175 = new Palavra("Muká", "Espingarda");
        databaseRef.child("palavra175").setValue(palavra175);

        Palavra palavra175_175 = new Palavra("muká", "espingarda");
        databaseRef.child("palavra175_175").setValue(palavra175_175);

        Palavra palavra176 = new Palavra("Mani'ay", "Goma de tapioca");
        databaseRef.child("palavra176").setValue(palavra176);

        Palavra palavra176_176 = new Palavra("mani'ay", "goma de tapioca");
        databaseRef.child("palavra176_176").setValue(palavra176_176);

        Palavra palavra177 = new Palavra("Man'ype", "Crueira");
        databaseRef.child("palavra177").setValue(palavra177);

        Palavra palavra177_177 = new Palavra("man'ype", "crueira");
        databaseRef.child("palavra177_177").setValue(palavra177_177);

        Palavra palavra178 = new Palavra("Morékuat", "Cacique");
        databaseRef.child("palavra178").setValue(palavra178);

        Palavra palavra178_178 = new Palavra("morékuat", "cacique");
        databaseRef.child("palavra178_178").setValue(palavra178_178);

        Palavra palavra179 = new Palavra("Og", "Guarda-chuva");
        databaseRef.child("palavra179").setValue(palavra179);

        Palavra palavra179_179 = new Palavra("og", "guarda-chuva");
        databaseRef.child("palavra179_179").setValue(palavra179_179);

        Palavra palavra180 = new Palavra("Pinã", "Anzol");
        databaseRef.child("palavra180").setValue(palavra180);

        Palavra palavra180_180 = new Palavra("pinã", "anzol");
        databaseRef.child("palavra180_180").setValue(palavra180_180);

        Palavra palavra181 = new Palavra("Paigni", "Pajé");
        databaseRef.child("palavra181").setValue(palavra181);

        Palavra palavra181_181 = new Palavra("paigni", "pajé");
        databaseRef.child("palavra181_181").setValue(palavra181_181);

        Palavra palavra182 = new Palavra("Panene", "Peneira");
        databaseRef.child("palavra182").setValue(palavra182);

        Palavra palavra182_182 = new Palavra("panene", "peneira");
        databaseRef.child("palavra182_182").setValue(palavra182_182);

        Palavra palavra183 = new Palavra("Puratig", "Porantin");
        databaseRef.child("palavra183").setValue(palavra183);

        Palavra palavra183_183 = new Palavra("puratig", "porantin");
        databaseRef.child("palavra183_183").setValue(palavra183_183);

        Palavra palavra184 = new Palavra("Pátu", "Gareira");
        databaseRef.child("palavra184").setValue(palavra184);

        Palavra palavra184_184 = new Palavra("pátu", "gareira");
        databaseRef.child("palavra184_184").setValue(palavra184_184);

        Palavra palavra185 = new Palavra("Patawi", "Suporte de cuia");
        databaseRef.child("palavra185").setValue(palavra185);

        Palavra palavra185_185 = new Palavra("patawi", "suporte de cuia");
        databaseRef.child("palavra185_185").setValue(palavra185_185);

        Palavra palavra186 = new Palavra("Púre", "Tarú");
        databaseRef.child("palavra186").setValue(palavra186);

        Palavra palavra186_186 = new Palavra("púre", "tarú");
        databaseRef.child("palavra186_186").setValue(palavra186_186);

        Palavra palavra187 = new Palavra("Sáripe", "Luva de tucandeira");
        databaseRef.child("palavra187").setValue(palavra187);

        Palavra palavra187_187 = new Palavra("sáripe", "luva de tucandeira");
        databaseRef.child("palavra187_187").setValue(palavra187_187);

        Palavra palavra188 = new Palavra("Súhu", "Fumo");
        databaseRef.child("palavra188").setValue(palavra188);

        Palavra palavra188_188 = new Palavra("súhu", "fumo");
        databaseRef.child("palavra188_188").setValue(palavra188_188);

        Palavra palavra189 = new Palavra("Sapo", "Guaraná ralado");
        databaseRef.child("palavra189").setValue(palavra189);

        Palavra palavra189_189 = new Palavra("sapo", "guaraná ralado");
        databaseRef.child("palavra189_189").setValue(palavra189_189);

        Palavra palavra190 = new Palavra("Táwa", "Comunidade");
        databaseRef.child("palavra190").setValue(palavra190);

        Palavra palavra190_190 = new Palavra("táwa", "comunidade");
        databaseRef.child("palavra190_190").setValue(palavra190_190);

        Palavra palavra191 = new Palavra("Úku", "Timbó");
        databaseRef.child("palavra191").setValue(palavra191);

        Palavra palavra191_191 = new Palavra("úku", "timbó");
        databaseRef.child("palavra191_191").setValue(palavra191_191);

        Palavra palavra192 = new Palavra("Úwa", "Flecha");
        databaseRef.child("palavra192").setValue(palavra192);

        Palavra palavra192_192 = new Palavra("úwa", "flecha");
        databaseRef.child("palavra192_192").setValue(palavra192_192);

        Palavra palavra193 = new Palavra("Waranã", "Guaraná");
        databaseRef.child("palavra193").setValue(palavra193);

        Palavra palavra193_193 = new Palavra("waranã", "guaraná");
        databaseRef.child("palavra193_193").setValue(palavra193_193);

        Palavra palavra194 = new Palavra("Wegku'a", "Pilão");
        databaseRef.child("palavra194").setValue(palavra194);

        Palavra palavra194_194 = new Palavra("wegku'a", "pilão");
        databaseRef.child("palavra194_194").setValue(palavra194_194);

        Palavra palavra195 = new Palavra("Wa'ã", "Panela");
        databaseRef.child("palavra195").setValue(palavra195);

        Palavra palavra195_195 = new Palavra("wa'ã", "panela");
        databaseRef.child("palavra195_195").setValue(palavra195_195);

        Palavra palavra196 = new Palavra("Yrysakag", "Paneiro");
        databaseRef.child("palavra196").setValue(palavra196);

        Palavra palavra196_196 = new Palavra("yrysakag", "paneiro");
        databaseRef.child("palavra196_196").setValue(palavra196_196);

        Palavra palavra197 = new Palavra("Yni", "Rede");
        databaseRef.child("palavra197").setValue(palavra197);

        Palavra palavra197_197 = new Palavra("yni", "rede");
        databaseRef.child("palavra197_197").setValue(palavra197_197);

        Palavra palavra198 = new Palavra("Yhape", "Colher de pau");
        databaseRef.child("palavra198").setValue(palavra198);

        Palavra palavra198_198 = new Palavra("yhape", "colher de pau");
        databaseRef.child("palavra198_198").setValue(palavra198_198);

        Palavra palavra199 = new Palavra("Yara", "Canoa");
        databaseRef.child("palavra199").setValue(palavra199);

        Palavra palavra199_199 = new Palavra("yara", "canoa");
        databaseRef.child("palavra199_199").setValue(palavra199_199);

        Palavra palavra200 = new Palavra("Awati", "Milho");
        databaseRef.child("palavra200").setValue(palavra200);

        Palavra palavra200_200 = new Palavra("awati", "milho");
        databaseRef.child("palavra200_200").setValue(palavra200_200);

        Palavra palavra201 = new Palavra("Hawuhu'i", "Bacaba");
        databaseRef.child("palavra201").setValue(palavra201);

        Palavra palavra201_201 = new Palavra("hawuhu'i", "bacaba");
        databaseRef.child("palavra201_201").setValue(palavra201_201);

        Palavra palavra202 = new Palavra("Kásu", "Cajú");
        databaseRef.child("palavra202").setValue(palavra202);

        Palavra palavra202_202 = new Palavra("kásu", "cajú");
        databaseRef.child("palavra202_202").setValue(palavra202_202);

        Palavra palavra203 = new Palavra("Miriti", "Buriti");
        databaseRef.child("palavra203").setValue(palavra203);

        Palavra palavra203_203 = new Palavra("miriti", "buriti");
        databaseRef.child("palavra203_203").setValue(palavra203_203);

        Palavra palavra204 = new Palavra("Mokiu", "Ingá");
        databaseRef.child("palavra204").setValue(palavra204);

        Palavra palavra204_204 = new Palavra("mokiu", "ingá");
        databaseRef.child("palavra204_204").setValue(palavra204_204);

        Palavra palavra205 = new Palavra("Magka", "Manga");
        databaseRef.child("palavra205").setValue(palavra205);

        Palavra palavra205_205 = new Palavra("magka", "manga");
        databaseRef.child("palavra205_205").setValue(palavra205_205);

        Palavra palavra206 = new Palavra("Nanã", "Abacaxi");
        databaseRef.child("palavra206").setValue(palavra206);

        Palavra palavra206_206 = new Palavra("nanã", "abacaxi");
        databaseRef.child("palavra206_206").setValue(palavra206_206);

        Palavra palavra207 = new Palavra("Pakua", "Banana");
        databaseRef.child("palavra207").setValue(palavra207);

        Palavra palavra207_207 = new Palavra("pakua", "banana");
        databaseRef.child("palavra207_207").setValue(palavra207_207);

        Palavra palavra208 = new Palavra("Sasym", "Laranja");
        databaseRef.child("palavra208").setValue(palavra208);

        Palavra palavra208_208 = new Palavra("sasym", "laranja");
        databaseRef.child("palavra208_208").setValue(palavra208_208);

        Palavra palavra209 = new Palavra("Wasa'i", "Açaí");
        databaseRef.child("palavra209").setValue(palavra209);

        Palavra palavra209_209 = new Palavra("wasa'i", "açaí");
        databaseRef.child("palavra209_209").setValue(palavra209_209);

        Palavra palavra210 = new Palavra("Waiawa", "Goiaba");
        databaseRef.child("palavra210").setValue(palavra210);

        Palavra palavra210_210 = new Palavra("waiawa", "goiaba");
        databaseRef.child("palavra210_210").setValue(palavra210_210);

        Palavra palavra211 = new Palavra("Wiriwa", "Beribá");
        databaseRef.child("palavra211").setValue(palavra211);

        Palavra palavra211_211 = new Palavra("wiriwa", "beribá");
        databaseRef.child("palavra211_211").setValue(palavra211_211);

        Palavra palavra212 = new Palavra("Át", "Sol");
        databaseRef.child("palavra212").setValue(palavra212);

        Palavra palavra212_212 = new Palavra("át", "sol");
        databaseRef.child("palavra212_212").setValue(palavra212_212);

        Palavra palavra213 = new Palavra("Awai'a", "Cará");
        databaseRef.child("palavra213").setValue(palavra213);

        Palavra palavra213_213 = new Palavra("awai'a", "cará");
        databaseRef.child("palavra213_213").setValue(palavra213_213);

        Palavra palavra214 = new Palavra("Ahiag", "Diabo");
        databaseRef.child("palavra214").setValue(palavra214);

        Palavra palavra214_214 = new Palavra("ahiag", "diabo");
        databaseRef.child("palavra214_214").setValue(palavra214_214);

        Palavra palavra215 = new Palavra("Amyap", "Banco");
        databaseRef.child("palavra215").setValue(palavra215);

        Palavra palavra215_215 = new Palavra("amyap", "banco");
        databaseRef.child("palavra215_215").setValue(palavra215_215);

        Palavra palavra216 = new Palavra("Ga'apy", "Floresta");
        databaseRef.child("palavra216").setValue(palavra216);

        Palavra palavra216_216 = new Palavra("ga'apy", "floresta");
        databaseRef.child("palavra216_216").setValue(palavra216_216);

        Palavra palavra217 = new Palavra("Iesui", "Jesus");
        databaseRef.child("palavra217").setValue(palavra217);

        Palavra palavra217_217 = new Palavra("iesui", "jesus");
        databaseRef.child("palavra217_217").setValue(palavra217_217);

        Palavra palavra218 = new Palavra("Ja'agkap", "Imagem");
        databaseRef.child("palavra218").setValue(palavra218);

        Palavra palavra218_218 = new Palavra("ja'agkap", "imagem");
        databaseRef.child("palavra218_218").setValue(palavra218_218);

        Palavra palavra219 = new Palavra("Morékuat", "Autoridade");
        databaseRef.child("palavra219").setValue(palavra219);

        Palavra palavra219_219 = new Palavra("morékuat", "autoridade");
        databaseRef.child("palavra219_219").setValue(palavra219_219);

        Palavra palavra220 = new Palavra("Mohag", "Remédio");
        databaseRef.child("palavra220").setValue(palavra220);

        Palavra palavra220_220 = new Palavra("mohag", "remédio");
        databaseRef.child("palavra220_220").setValue(palavra220_220);

        Palavra palavra221 = new Palavra("Nu", "Pedra");
        databaseRef.child("palavra221").setValue(palavra221);

        Palavra palavra221_221 = new Palavra("nu", "pedra");
        databaseRef.child("palavra221_221").setValue(palavra221_221);

        Palavra palavra222 = new Palavra("Nem", "Podre");
        databaseRef.child("palavra222").setValue(palavra222);

        Palavra palavra222_222 = new Palavra("nem", "podre");
        databaseRef.child("palavra222_222").setValue(palavra222_222);

        Palavra palavra223 = new Palavra("Pa'i", "Pastor");
        databaseRef.child("palavra223").setValue(palavra223);

        Palavra palavra223_223 = new Palavra("pa'i", "pastor");
        databaseRef.child("palavra223_223").setValue(palavra223_223);

        Palavra palavra224 = new Palavra("Puruwei", "Professor");
        databaseRef.child("palavra224").setValue(palavra224);

        Palavra palavra224_224 = new Palavra("puruwei", "professor");
        databaseRef.child("palavra224_224").setValue(palavra224_224);

        Palavra palavra225 = new Palavra("Puruweira", "Professora");
        databaseRef.child("palavra225").setValue(palavra225);

        Palavra palavra225_225 = new Palavra("puruweira", "professora");
        databaseRef.child("palavra225_225").setValue(palavra225_225);

        Palavra palavra226 = new Palavra("Surara", "Soldado");
        databaseRef.child("palavra226").setValue(palavra226);

        Palavra palavra226_226 = new Palavra("surara", "soldado");
        databaseRef.child("palavra226_226").setValue(palavra226_226);

        Palavra palavra227 = new Palavra("Seko", "Costume");
        databaseRef.child("palavra227").setValue(palavra227);

        Palavra palavra227_227 = new Palavra("seko", "costume");
        databaseRef.child("palavra227_227").setValue(palavra227_227);

        Palavra palavra228 = new Palavra("Tupana", "Deus");
        databaseRef.child("palavra228").setValue(palavra228);

        Palavra palavra228_228 = new Palavra("tupana", "deus");
        databaseRef.child("palavra228_228").setValue(palavra228_228);

        Palavra palavra229 = new Palavra("Waikiru", "Estrela");
        databaseRef.child("palavra229").setValue(palavra229);

        Palavra palavra229_229 = new Palavra("waikiru", "estrela");
        databaseRef.child("palavra229_229").setValue(palavra229_229);

        Palavra palavra230 = new Palavra("Wáty", "Lua");
        databaseRef.child("palavra230").setValue(palavra230);

        Palavra palavra230_230 = new Palavra("wáty", "lua");
        databaseRef.child("palavra230_230").setValue(palavra230_230);

        Palavra palavra231 = new Palavra("Wá'yp", "Arco-íris");
        databaseRef.child("palavra231").setValue(palavra231);

        Palavra palavra231_231 = new Palavra("wá'yp", "arco-íris");
        databaseRef.child("palavra231_231").setValue(palavra231_231);

        Palavra palavra232 = new Palavra("Y'y", "Água");
        databaseRef.child("palavra232").setValue(palavra232);

        Palavra palavra232_232 = new Palavra("y'y", "água");
        databaseRef.child("palavra232_232").setValue(palavra232_232);

        Palavra palavra233 = new Palavra("Ywyhig", "Nuvem");
        databaseRef.child("palavra233").setValue(palavra233);

        Palavra palavra233_233 = new Palavra("ywyhig", "nuvem");
        databaseRef.child("palavra233_233").setValue(palavra233_233);

        Palavra palavra234 = new Palavra("Mahyt", "Cachaça");
        databaseRef.child("palavra234").setValue(palavra234);

        Palavra palavra234_234 = new Palavra("mahyt", "cachaça");
        databaseRef.child("palavra234_234").setValue(palavra234_234);

        Palavra palavra235 = new Palavra("Miú", "Comida");
        databaseRef.child("palavra235").setValue(palavra235);

        Palavra palavra235_235 = new Palavra("miú", "comida");
        databaseRef.child("palavra235_235").setValue(palavra235_235);

        Palavra palavra236 = new Palavra("Yaman", "Chuva");
        databaseRef.child("palavra236").setValue(palavra236);

        Palavra palavra236_236 = new Palavra("yaman", "chuva");
        databaseRef.child("palavra236_236").setValue(palavra236_236);

        Palavra palavra237 = new Palavra("Wu'uka", "Brigar");
        databaseRef.child("palavra237").setValue(palavra237);

        Palavra palavra237_237 = new Palavra("wu'uka", "brigar");
        databaseRef.child("palavra237_237").setValue(palavra237_237);

        Palavra palavra238 = new Palavra("Iwato", "Grande");
        databaseRef.child("palavra238").setValue(palavra238);

        Palavra palavra238_238 = new Palavra("iwato", "grande");
        databaseRef.child("palavra238_238").setValue(palavra238_238);

        Palavra palavra239 = new Palavra("Kurim", "Pequeno");
        databaseRef.child("palavra239").setValue(palavra239);

        Palavra palavra239_239 = new Palavra("kurim", "pequeno");
        databaseRef.child("palavra238_239").setValue(palavra239_239);

    }


}