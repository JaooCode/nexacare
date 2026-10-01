package com.nexatech.nexacare.model;

public enum StatusAgendamento {
    AGENDADA("Agendada"), CONFIRMADA("Confirmada"), REALIZADA("Realizada"), CANCELADA("Cancelada");

    private final String rotulo;

    StatusAgendamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Consulta ainda ocupa o horário na agenda. */
    public boolean isAtiva() {
        return this == AGENDADA || this == CONFIRMADA;
    }
}
