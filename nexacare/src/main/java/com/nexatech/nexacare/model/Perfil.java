package com.nexatech.nexacare.model;

public enum Perfil {
    ADMINISTRADOR, RECEPCIONISTA, PROFISSIONAL, PACIENTE;

    public boolean isEquipe() {
        return this != PACIENTE;
    }
}
