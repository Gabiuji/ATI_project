package com.example.atiproject.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.atiproject.FormLogin;
import com.example.atiproject.R;
import com.example.atiproject.TelaPrincipal;
import com.example.atiproject.suporte;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ConfigFragment extends Fragment {


    private TextView nomeUsuario, emailUsuario;

    private Button btDeslogar, btSuporte, btNao, btSim;

    View vista;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String usuarioID;

    public ConfigFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ConfigFragment newInstance(String param1, String param2) {
        ConfigFragment fragment = new ConfigFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        vista = inflater.inflate(R.layout.fragment_config, container, false);
        btDeslogar = vista.findViewById(R.id.bt_deslogar);
        btSuporte = vista.findViewById(R.id.bt_suporte);
        nomeUsuario = vista.findViewById(R.id.text_user);
        emailUsuario = vista.findViewById(R.id.emailUsuario);

        btSuporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), suporte.class);
                startActivity(intent);
            }
        });
        btDeslogar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(getContext());
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                dialog.setContentView(R.layout.dialog_longout);

                dialog.findViewById(R.id.bt_sim).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(getContext(), FormLogin.class);
                        startActivity(intent);
                    }
                });

                dialog.findViewById(R.id.bt_nao).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent2 = new Intent(getContext(), TelaPrincipal.class);
                        startActivity(intent2);
                    }
                });
                dialog.show();
            }
        });
        
        return vista;
    }

    @Override
    public void onStart() {
        super.onStart();

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        usuarioID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference documentReference = db.collection("usuarios").document(usuarioID);
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                if (documentSnapshot != null) {
                    nomeUsuario.setText(documentSnapshot.getString("nome"));
                    emailUsuario.setText(email);
                }
            }
        });
    }

}