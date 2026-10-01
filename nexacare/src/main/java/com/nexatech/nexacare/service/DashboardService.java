package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.AgendamentoResponse;
import com.nexatech.nexacare.dto.AgendaDtos.DashboardResponse;
import com.nexatech.nexacare.dto.AgendaDtos.GradeProfissional;
import com.nexatech.nexacare.model.Agendamento;
import com.nexatech.nexacare.model.StatusAgendamento;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.PacienteRepository;
import com.nexatech.nexacare.repository.ProfissionalRepository;
import com.nexatech.nexacare.repository.UsuarioRepository;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.nexatech.nexacare.model.Perfil.*;
import static com.nexatech.nexacare.service.AgendaService.ATIVAS;

/** Indicadores do dashboard, sempre restritos ao que o perfil pode ver. */
@Service
public class DashboardService {
    private static final List<StatusAgendamento> NAO_CANCELADAS =
            Arrays.stream(StatusAgendamento.values()).filter(s -> s != StatusAgendamento.CANCELADA).toList();
    private static final LocalDate DATA_MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAX = LocalDate.of(2999, 12, 31);

    private final AgendamentoRepository agendamentos;
    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;
    private final UsuarioRepository usuarios;
    private final AgendaService agenda;
    private final NotificacaoService notificacoes;
    private final Mapeador mapeador;

    public DashboardService(AgendamentoRepository agendamentos, PacienteRepository pacientes,
                            ProfissionalRepository profissionais, UsuarioRepository usuarios, AgendaService agenda,
                            NotificacaoService notificacoes, Mapeador mapeador) {
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.usuarios = usuarios;
        this.agenda = agenda;
        this.notificacoes = notificacoes;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public DashboardResponse montar(UsuarioLogado u) {
        long profId = u.is(PROFISSIONAL) ? u.profissionalId() : 0L;
        long pacId = u.is(PACIENTE) ? u.pacienteId() : 0L;
        LocalDate hoje = LocalDate.now();

        long consultasHoje = contar(hoje, hoje, NAO_CANCELADAS, profId, pacId);
        long semana = contar(hoje, hoje.plusDays(6), NAO_CANCELADAS, profId, pacId);
        long canceladas = contar(DATA_MIN, DATA_MAX, List.of(StatusAgendamento.CANCELADA), profId, pacId);
        long realizadas = contar(DATA_MIN, DATA_MAX, List.of(StatusAgendamento.REALIZADA), profId, pacId);
        long total = contar(DATA_MIN, DATA_MAX, List.of(StatusAgendamento.values()), profId, pacId);

        List<AgendamentoResponse> proximas = agendamentos
                .filtrar(hoje, hoje.plusDays(60), ATIVAS, profId, pacId, "%").stream()
                .filter(a -> !LocalDateTime.of(a.getData(), a.getHora()).isBefore(LocalDateTime.now()))
                .limit(6).map(mapeador::agendamento).toList();

        boolean gestao = u.is(ADMINISTRADOR, RECEPCIONISTA);
        Long livres = null;
        if (!u.is(PACIENTE)) {
            livres = agenda.grade(hoje, null, u).profissionais().stream().mapToLong(this::livres).sum();
        }
        return new DashboardResponse(u.perfil().name(), consultasHoje, semana, canceladas, realizadas,
                gestao ? pacientes.countByAtivoTrue() : null,
                gestao ? profissionais.countByAtivoTrue() : null,
                livres,
                gestao ? agendamentos.countByRemarcacaoSolicitadaTrueAndStatusIn(ATIVAS) : null,
                u.is(ADMINISTRADOR) ? usuarios.count() : null,
                gestao && total > 0 ? (int) Math.round(canceladas * 100.0 / total) : null,
                notificacoes.naoLidas(u), proximas);
    }

    private long contar(LocalDate ini, LocalDate fim, List<StatusAgendamento> st, long profId, long pacId) {
        return agendamentos.filtrar(ini, fim, st, profId, pacId, "%").size();
    }

    private long livres(GradeProfissional g) {
        return g.slots().stream().filter(s -> "LIVRE".equals(s.estado())).count();
    }
}
