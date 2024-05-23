package com.example.atiproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;

import com.example.atiproject.databinding.ActivityTelaPrincipalBinding;

public class TelaPrincipal extends AppCompatActivity {

    private ActivityTelaPrincipalBinding binding;

    private NavHostFragment navHostFragment;
    private NavController navController;

    public void onBackPressed(){
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTelaPrincipalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initNavigation();

    }
    private void initNavigation(){
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController =navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.btnNagigation, navController);
    }
}