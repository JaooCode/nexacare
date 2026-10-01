package com.nexatech.nexacare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracoes")
public class Configuracao {
    @Id
    @Column(length = 50)
    private String chave;

    @Column(nullable = false, length = 100)
    private String valor;

    protected Configuracao() {}

    public Configuracao(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() { return chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
