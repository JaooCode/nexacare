package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.AgendamentoRequest;
import com.nexatech.nexacare.dto.AgendaDtos.AgendamentoResponse;
import com.nexatech.nexacare.exception.ExcecoesNegocio.*;
import com.nexatech.nexacare.model.*;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.PacienteRepository;
import com.nexatech.nexacare.repository.ProfissionalRepository;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.nexatech.nexacare.model.Perfil.*;
import static com.nexatech.nexacare.service.AgendaService.ATIVAS;

/** Regras de negócio dos agendamentos. */
@Service
public class AgendamentoService {
    private static final LocalDate DATA_MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAX = LocalDate.of(2999, 12, 31);

    private final AgendamentoRepository repo;
    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;
    private final ConfiguracaoService config;
    private final NotificacaoService notificacoes;
    private final Mapeador mapeador;

    public AgendamentoService(AgendamentoRepository repo, PacienteRepository pacientes,
                              ProfissionalRepository profissionais, ConfiguracaoService config,
                              NotificacaoService notificacoes, Mapeador mapeador) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.config = config;
        this.notificacoes = notificacoes;
        this.mapeador = mapeador;
    }

    // ---------- Consultas ----------

    /**
     * Sem filtro de status, consultas canceladas ficam de fora (não aparecem como ativas);
     * use status=CANCELADA ou status=TODAS para vê-las.
     */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(LocalDate inicio, LocalDate fim, Long profissionalId, Long pacienteId,
                                            String status, String busca, UsuarioLogado u) {
        List<StatusAgendamento> statuses;
        if (status == null || status.isBlank()) {
            statuses = Arrays.stream(StatusAgendamento.values())
                    .filter(s -> s != StatusAgendamento.CANCELADA).toList();
        } else if ("TODAS".equalsIgnoreCase(status)) {
            statuses = List.of(StatusAgendamento.values());
        } else {
            statuses = List.of(StatusAgendamento.valueOf(status.toUpperCase()));
        }
        // Escopo por perfil: profissional só vê a própria agenda; paciente só vê as próprias consultas.
        long profId = u.is(PROFISSIONAL) ? u.profissionalId() : (profissionalId == null ? 0L : profissionalId);
        long pacId = u.is(PACIENTE) ? u.pacienteId() : (pacienteId == null ? 0L : pacienteId);

        return repo.filtrar(inicio == null ? DATA_MIN : inicio, fim == null ? DATA_MAX : fim, statuses,
                        profId, pacId, Validacoes.padraoBusca(busca))
                .stream().map(mapeador::agendamento).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id, UsuarioLogado u) {
        return mapeador.agendamento(obterComEscopo(id, u));
    }

    // ---------- Criação e remarcação ----------

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest r, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA, PACIENTE);
        Long pacienteId = u.is(PACIENTE) ? u.pacienteId() : r.pacienteId();
        if (pacienteId == null) throw new RegraNegocioException("Selecione o paciente.");

        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado."));
        if (!paciente.isAtivo()) throw new RegraNegocioException("O cadastro deste paciente está desativado.");
        Profissional profissional = profissionalParaAgendar(r);

        validarDataHora(r.data(), r.hora());
        validarConflitos(profissional.getId(), paciente.getId(), r.data(), r.hora(), 0L);

        Agendamento a = new Agendamento();
        a.setPaciente(paciente);
        a.setProfissional(profissional);
        a.setData(r.data());
        a.setHora(r.hora());
        a.setObservacao(limpar(r.observacao()));
        repo.save(a);

        notificacoes.registrar(a, TipoNotificacao.CONFIRMACAO, "Olá, " + paciente.getNome()
                + "! Sua consulta com " + NotificacaoService.descricao(a)
                + " foi agendada. Confirme sua presença pelo sistema ou responda esta mensagem.");
        return mapeador.agendamento(a);
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest r, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA);
        Agendamento a = obterComEscopo(id, u);
        if (!a.getStatus().isAtiva()) {
            throw new RegraNegocioException("Somente consultas agendadas ou confirmadas podem ser alteradas.");
        }
        Paciente paciente = r.pacienteId() == null ? a.getPaciente() : pacientes.findById(r.pacienteId())
                .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado."));
        if (!paciente.isAtivo()) throw new RegraNegocioException("O cadastro deste paciente está desativado.");
        Profissional profissional = profissionalParaAgendar(r);

        boolean mudouHorario = !profissional.getId().equals(a.getProfissional().getId())
                || !r.data().equals(a.getData()) || !r.hora().equals(a.getHora());
        if (mudouHorario) validarDataHora(r.data(), r.hora());
        validarConflitos(profissional.getId(), paciente.getId(), r.data(), r.hora(), a.getId());

        a.setPaciente(paciente);
        a.setProfissional(profissional);
        a.setData(r.data());
        a.setHora(r.hora());
        a.setObservacao(limpar(r.observacao()));
        if (mudouHorario) {
            a.setStatus(StatusAgendamento.AGENDADA); // precisa ser confirmada novamente
            a.setRemarcacaoSolicitada(false);
            notificacoes.registrar(a, TipoNotificacao.ALTERACAO, "Olá, " + paciente.getNome()
                    + "! Sua consulta foi remarcada para " + NotificacaoService.descricao(a)
                    + ". Por favor, confirme a nova data.");
        }
        return mapeador.agendamento(a);
    }

    // ---------- Status ----------

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusAgendamento novo, UsuarioLogado u) {
        Agendamento a = obterComEscopo(id, u);
        switch (novo) {
            case CONFIRMADA -> {
                u.exigir(RECEPCIONISTA, PROFISSIONAL, PACIENTE);
                if (a.getStatus() != StatusAgendamento.AGENDADA) {
                    throw new RegraNegocioException("Apenas consultas com status Agendada podem ser confirmadas.");
                }
                a.setStatus(StatusAgendamento.CONFIRMADA);
                a.setRemarcacaoSolicitada(false);
                notificacoes.registrar(a, TipoNotificacao.CONFIRMACAO, "Consulta de " + a.getPaciente().getNome()
                        + " confirmada: " + NotificacaoService.descricao(a) + ".");
            }
            case REALIZADA -> {
                u.exigir(PROFISSIONAL);
                if (!a.getStatus().isAtiva()) {
                    throw new RegraNegocioException("Esta consulta não pode ser marcada como realizada.");
                }
                if (a.getData().isAfter(LocalDate.now())) {
                    throw new RegraNegocioException(
                            "Só é possível marcar como realizada uma consulta de hoje ou de datas passadas.");
                }
                a.setStatus(StatusAgendamento.REALIZADA);
            }
            case CANCELADA -> {
                u.exigir(RECEPCIONISTA, PROFISSIONAL, PACIENTE);
                if (!a.getStatus().isAtiva()) {
                    throw new RegraNegocioException("Esta consulta não pode mais ser cancelada.");
                }
                a.setStatus(StatusAgendamento.CANCELADA);
                a.setRemarcacaoSolicitada(false);
                notificacoes.registrar(a, TipoNotificacao.CANCELAMENTO, "A consulta de "
                        + a.getPaciente().getNome() + " com " + NotificacaoService.descricao(a)
                        + " foi cancelada. O horário ficou disponível para novo agendamento.");
            }
            default -> throw new RegraNegocioException("Alteração de status não permitida.");
        }
        return mapeador.agendamento(a);
    }

    @Transactional
    public AgendamentoResponse solicitarRemarcacao(Long id, UsuarioLogado u) {
        u.exigir(PACIENTE);
        Agendamento a = obterComEscopo(id, u);
        if (!a.getStatus().isAtiva()) {
            throw new RegraNegocioException("Só é possível solicitar remarcação de consultas ativas.");
        }
        if (a.isRemarcacaoSolicitada()) {
            throw new RegraNegocioException("A remarcação já foi solicitada. Aguarde o contato da clínica.");
        }
        a.setRemarcacaoSolicitada(true);
        notificacoes.registrar(a, TipoNotificacao.SOLICITACAO_REMARCACAO, a.getPaciente().getNome()
                + " solicitou remarcação da consulta com " + NotificacaoService.descricao(a)
                + ". A recepção deve entrar em contato para definir o novo horário.");
        return mapeador.agendamento(a);
    }

    // ---------- Auxiliares ----------

    private Agendamento obterComEscopo(Long id, UsuarioLogado u) {
        Agendamento a = repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada."));
        boolean fora = (u.is(PROFISSIONAL) && !a.getProfissional().getId().equals(u.profissionalId()))
                || (u.is(PACIENTE) && !a.getPaciente().getId().equals(u.pacienteId()));
        if (fora) throw new AcessoNegadoException();
        return a;
    }

    /** Busca o profissional com trava de linha para serializar agendamentos simultâneos do mesmo profissional. */
    private Profissional profissionalParaAgendar(AgendamentoRequest r) {
        Profissional p = profissionais.buscarComTrava(r.profissionalId())
                .orElseThrow(() -> new RegraNegocioException("Profissional não encontrado."));
        if (!p.isAtivo()) throw new RegraNegocioException("Este profissional está inativo.");
        if (r.especialidadeId() != null && !r.especialidadeId().equals(p.getEspecialidade().getId())) {
            throw new RegraNegocioException("O profissional selecionado não atende a especialidade informada.");
        }
        return p;
    }

    private void validarDataHora(LocalDate data, LocalTime hora) {
        if (data.isBefore(LocalDate.now()) || data.isAfter(LocalDate.now().plusYears(2))) {
            throw new RegraNegocioException("Data inválida. Escolha uma data a partir de hoje (até 2 anos à frente).");
        }
        if (!AgendaService.diaUtil(data)) {
            throw new RegraNegocioException("A clínica atende apenas de segunda a sexta-feira.");
        }
        if (!config.horarios().contains(hora)) {
            throw new RegraNegocioException("Horário inválido. Escolha um dos horários de atendimento disponíveis.");
        }
        if (LocalDateTime.of(data, hora).isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Este horário já passou. Escolha um horário futuro.");
        }
    }

    private void validarConflitos(Long profissionalId, Long pacienteId, LocalDate data, LocalTime hora, Long ignorarId) {
        if (repo.conflitosDoProfissional(profissionalId, data, hora, ATIVAS, ignorarId) > 0) {
            throw new ConflitoException("Este horário já está ocupado.");
        }
        if (repo.conflitosDoPaciente(pacienteId, data, hora, ATIVAS, ignorarId) > 0) {
            throw new ConflitoException("O paciente já possui uma consulta neste mesmo horário.");
        }
    }

    private String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
