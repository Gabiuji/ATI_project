package com.example.atiproject;

public class Palavra {
    private String palavraOriginal;
    private String traducao;

    public Palavra() {
        // Construtor vazio necessário para o Firebase Realtime Database
    }

    public Palavra(String palavraOriginal, String traducao) {
        this.palavraOriginal = palavraOriginal;
        this.traducao = traducao;
    }

    public String getPalavraOriginal() {
        return palavraOriginal;
    }

    public String getTraducao() {
        return traducao;
    }
}

