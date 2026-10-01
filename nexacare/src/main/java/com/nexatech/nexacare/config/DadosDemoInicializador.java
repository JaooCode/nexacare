package com.nexatech.nexacare.config;

import com.nexatech.nexacare.model.*;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.PacienteRepository;
import com.nexatech.nexacare.repository.ProfissionalRepository;
import com.nexatech.nexacare.service.AgendaService;
import com.nexatech.nexacare.service.NotificacaoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cria consultas e notificações FICTÍCIAS de exemplo na primeira execução (tabela vazia).
 * As datas são calculadas a partir de hoje, sempre em dias úteis, para a demonstração nunca ficar "vazia".
 */
@Component
public class DadosDemoInicializador implements CommandLineRunner {
    private final AgendamentoRepository agendamentos;
    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;
    private final NotificacaoService notificacoes;

    public DadosDemoInicializador(AgendamentoRepository agendamentos, PacienteRepository pacientes,
                                  ProfissionalRepository profissionais, NotificacaoService notificacoes) {
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.notificacoes = notificacoes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (agendamentos.count() > 0 || pacientes.count() == 0 || profissionais.count() == 0) return;

        LocalDate base = diaUtil(LocalDate.now(), 0);

        // profissional, paciente, dia útil relativo a hoje, hora, status
        criar(1, 1, diaUtil(base, -2), "09:00", StatusAgendamento.REALIZADA, true);
        criar(2, 2, diaUtil(base, -1), "10:00", StatusAgendamento.REALIZADA, true);
        criar(3, 3, diaUtil(base, -3), "14:00", StatusAgendamento.CANCELADA, true);

        criar(1, 2, base, "08:30", StatusAgendamento.CONFIRMADA, false);
        criar(1, 3, base, "09:30", StatusAgendamento.AGENDADA, false);
        criar(1, 4, base, "10:30", StatusAgendamento.CONFIRMADA, false);
        criar(1, 5, base, "14:30", StatusAgendamento.AGENDADA, false);
        criar(2, 1, base, "09:00", StatusAgendamento.CONFIRMADA, false);
        criar(2, 7, base, "15:00", StatusAgendamento.AGENDADA, false);

        LocalDate amanha = diaUtil(base, 1);
        Agendamento consultaAna = criar(1, 1, amanha, "10:00", StatusAgendamento.AGENDADA, false);
        criar(4, 6, amanha, "09:00", StatusAgendamento.AGENDADA, false);
        criar(2, 5, amanha, "11:00", StatusAgendamento.AGENDADA, false);
        criar(1, 7, amanha, "15:00", StatusAgendamento.CANCELADA, false);
        notificacoes.registrar(consultaAna, TipoNotificacao.LEMBRETE, "Lembrete para Ana Beatriz Lima: você tem consulta com "
                + NotificacaoService.descricao(consultaAna) + ". Em caso de imprevisto, cancele ou remarque com antecedência.");

        criar(3, 3, diaUtil(base, 2), "10:30", StatusAgendamento.CONFIRMADA, false);
        Agendamento pedido = criar(4, 6, diaUtil(base, 3), "14:00", StatusAgendamento.AGENDADA, false);
        pedido.setRemarcacaoSolicitada(true);
        notificacoes.registrar(pedido, TipoNotificacao.SOLICITACAO_REMARCACAO, "Felipe Ramos Teixeira solicitou remarcação da consulta com "
                + NotificacaoService.descricao(pedido) + ". A recepção deve entrar em contato para definir o novo horário.");
        Agendamento alterada = criar(2, 3, diaUtil(base, 4), "16:00", StatusAgendamento.AGENDADA, false);
        notificacoes.registrar(alterada, TipoNotificacao.ALTERACAO, "Olá, Carla Mendes Rocha! Sua consulta foi remarcada para "
                + NotificacaoService.descricao(alterada) + ". Por favor, confirme a nova data.");
    }

    private Agendamento criar(long profissionalId, long pacienteId, LocalDate data, String hora,
                              StatusAgendamento status, boolean antiga) {
        Agendamento a = new Agendamento();
        a.setProfissional(profissionais.getReferenceById(profissionalId));
        a.setPaciente(pacientes.getReferenceById(pacienteId));
        a.setData(data);
        a.setHora(LocalTime.parse(hora));
        a.setStatus(status);
        agendamentos.save(a);

        Agendamento completo = agendamentos.findById(a.getId()).orElseThrow();
        Paciente p = completo.getPaciente();
        if (status == StatusAgendamento.CANCELADA) {
            registrar(completo, TipoNotificacao.CANCELAMENTO, "A consulta de " + p.getNome() + " com "
                    + NotificacaoService.descricao(completo) + " foi cancelada. O horário ficou disponível.", antiga);
        } else {
            registrar(completo, TipoNotificacao.CONFIRMACAO, "Olá, " + p.getNome() + "! Sua consulta com "
                    + NotificacaoService.descricao(completo) + " foi agendada. Confirme sua presença pelo sistema.", antiga);
        }
        return completo;
    }

    private void registrar(Agendamento a, TipoNotificacao tipo, String mensagem, boolean lida) {
        Notificacao n = notificacoes.registrar(a, tipo, mensagem);
        n.setLida(lida); // notificações de consultas passadas já aparecem como lidas
    }

    /** Avança (ou recua) N dias úteis a partir da data, ignorando sábados e domingos. */
    private static LocalDate diaUtil(LocalDate data, int deslocamento) {
        LocalDate d = data;
        int passo = deslocamento < 0 ? -1 : 1;
        int restantes = Math.abs(deslocamento);
        while (!AgendaService.diaUtil(d)) d = d.plusDays(1);
        while (restantes > 0) {
            d = d.plusDays(passo);
            if (AgendaService.diaUtil(d)) restantes--;
        }
        return d;
    }
}
