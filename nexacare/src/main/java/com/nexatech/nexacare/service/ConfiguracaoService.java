package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.ConfiguracaoSistema;
import com.nexatech.nexacare.exception.ExcecoesNegocio.RegraNegocioException;
import com.nexatech.nexacare.model.Configuracao;
import com.nexatech.nexacare.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Parâmetros de funcionamento da clínica (horário de atendimento, duração da consulta e pausa). */
@Service
public class ConfiguracaoService {
    private final ConfiguracaoRepository repo;

    public ConfiguracaoService(ConfiguracaoRepository repo) {
        this.repo = repo;
    }

    private String valor(String chave, String padrao) {
        return repo.findById(chave).map(Configuracao::getValor).orElse(padrao);
    }

    public ConfiguracaoSistema obter() {
        return new ConfiguracaoSistema(valor("horario_inicio", "08:00"), valor("horario_fim", "18:00"),
                Integer.parseInt(valor("duracao_minutos", "30")),
                valor("pausa_inicio", "12:00"), valor("pausa_fim", "13:00"));
    }

    @Transactional
    public ConfiguracaoSistema atualizar(ConfiguracaoSistema c) {
        LocalTime inicio = hora(c.inicio()), fim = hora(c.fim());
        LocalTime pausaInicio = hora(c.pausaInicio()), pausaFim = hora(c.pausaFim());
        if (!inicio.isBefore(fim)) {
            throw new RegraNegocioException("O horário de início deve ser anterior ao de término.");
        }
        if (c.duracaoMinutos() < 10 || c.duracaoMinutos() > 120) {
            throw new RegraNegocioException("A duração da consulta deve ficar entre 10 e 120 minutos.");
        }
        if (pausaFim.isBefore(pausaInicio)) {
            throw new RegraNegocioException("O fim da pausa deve ser posterior ao início.");
        }
        salvar("horario_inicio", c.inicio());
        salvar("horario_fim", c.fim());
        salvar("duracao_minutos", String.valueOf(c.duracaoMinutos()));
        salvar("pausa_inicio", c.pausaInicio());
        salvar("pausa_fim", c.pausaFim());
        return obter();
    }

    private void salvar(String chave, String valor) {
        Configuracao c = repo.findById(chave).orElse(new Configuracao(chave, valor));
        c.setValor(valor);
        repo.save(c);
    }

    private LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Horário inválido: " + texto);
        }
    }

    /** Todos os horários de início de consulta possíveis em um dia. */
    public List<LocalTime> horarios() {
        ConfiguracaoSistema c = obter();
        LocalTime fim = LocalTime.parse(c.fim());
        LocalTime pausaInicio = LocalTime.parse(c.pausaInicio()), pausaFim = LocalTime.parse(c.pausaFim());
        int min = c.duracaoMinutos();
        List<LocalTime> lista = new ArrayList<>();
        for (LocalTime t = LocalTime.parse(c.inicio());
             !t.plusMinutes(min).isAfter(fim) && t.plusMinutes(min).isAfter(t);
             t = t.plusMinutes(min)) {
            boolean naPausa = t.isBefore(pausaFim) && t.plusMinutes(min).isAfter(pausaInicio);
            if (!naPausa) lista.add(t);
        }
        return lista;
    }
}
