package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.GradeDia;
import com.nexatech.nexacare.dto.AgendaDtos.GradeProfissional;
import com.nexatech.nexacare.dto.AgendaDtos.Slot;
import com.nexatech.nexacare.model.Agendamento;
import com.nexatech.nexacare.model.Profissional;
import com.nexatech.nexacare.model.StatusAgendamento;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.ProfissionalRepository;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.nexatech.nexacare.model.Perfil.PACIENTE;
import static com.nexatech.nexacare.model.Perfil.PROFISSIONAL;

/** Monta a grade de horários (livres/ocupados) usada pela tela de agenda e pelo formulário de agendamento. */
@Service
public class AgendaService {
    public static final List<StatusAgendamento> ATIVAS =
            Arrays.stream(StatusAgendamento.values()).filter(StatusAgendamento::isAtiva).toList();

    private final AgendamentoRepository agendamentos;
    private final ProfissionalRepository profissionais;
    private final ConfiguracaoService config;

    public AgendaService(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
                         ConfiguracaoService config) {
        this.agendamentos = agendamentos;
        this.profissionais = profissionais;
        this.config = config;
    }

    public static boolean diaUtil(LocalDate data) {
        return data.getDayOfWeek() != DayOfWeek.SATURDAY && data.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    @Transactional(readOnly = true)
    public GradeDia grade(LocalDate data, Long profissionalId, UsuarioLogado u) {
        boolean util = diaUtil(data);
        List<Profissional> lista = profissionais.findByAtivoTrueOrderByNome().stream()
                .filter(p -> !u.is(PROFISSIONAL) || p.getId().equals(u.profissionalId()))
                .filter(p -> profissionalId == null || p.getId().equals(profissionalId))
                .toList();

        Map<String, Agendamento> ocupados = new HashMap<>();
        if (util) {
            for (Agendamento a : agendamentos.findByDataAndStatusIn(data, ATIVAS)) {
                ocupados.put(a.getProfissional().getId() + "|" + a.getHora(), a);
            }
        }
        List<LocalTime> horarios = util ? config.horarios() : List.of();
        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now();

        List<GradeProfissional> grades = new ArrayList<>();
        for (Profissional p : lista) {
            List<Slot> slots = new ArrayList<>();
            for (LocalTime h : horarios) {
                Agendamento a = ocupados.get(p.getId() + "|" + h);
                boolean passado = data.isBefore(hoje) || (data.equals(hoje) && !h.isAfter(agora));
                slots.add(a != null ? slotOcupado(a, u) : new Slot(h, passado ? "INDISPONIVEL" : "LIVRE", null, null, null));
            }
            grades.add(new GradeProfissional(p.getId(), p.getNome(), p.getEspecialidade().getNome(), slots));
        }
        return new GradeDia(data, util, grades);
    }

    /** Paciente enxerga apenas que o horário está ocupado, sem dados de outros pacientes (LGPD). */
    private Slot slotOcupado(Agendamento a, UsuarioLogado u) {
        boolean proprio = u.is(PACIENTE) && a.getPaciente().getId().equals(u.pacienteId());
        if (u.is(PACIENTE) && !proprio) {
            return new Slot(a.getHora(), "OCUPADO", null, null, null);
        }
        return new Slot(a.getHora(), "OCUPADO", a.getId(), a.getPaciente().getNome(), a.getStatus());
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(LocalDate data, Long profissionalId, UsuarioLogado u) {
        return grade(data, profissionalId, u).profissionais().stream().findFirst()
                .map(g -> g.slots().stream().filter(s -> "LIVRE".equals(s.estado())).map(Slot::hora).toList())
                .orElse(List.of());
    }
}
