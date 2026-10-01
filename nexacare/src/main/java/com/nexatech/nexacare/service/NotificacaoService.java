package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.NotificacaoResponse;
import com.nexatech.nexacare.exception.ExcecoesNegocio.AcessoNegadoException;
import com.nexatech.nexacare.exception.ExcecoesNegocio.NaoEncontradoException;
import com.nexatech.nexacare.model.*;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.NotificacaoRepository;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.nexatech.nexacare.model.Perfil.*;

/**
 * Notificações e lembretes SIMULADOS: ficam registrados no banco e aparecem na tela.
 * Nenhum e-mail/WhatsApp/SMS real é enviado.
 */
@Service
public class NotificacaoService {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacaoRepository repo;
    private final AgendamentoRepository agendamentos;
    private final Mapeador mapeador;

    public NotificacaoService(NotificacaoRepository repo, AgendamentoRepository agendamentos, Mapeador mapeador) {
        this.repo = repo;
        this.agendamentos = agendamentos;
        this.mapeador = mapeador;
    }

    public static String descricao(Agendamento a) {
        return a.getProfissional().getNome() + " (" + a.getProfissional().getEspecialidade().getNome()
                + ") em " + a.getData().format(DATA) + " às " + a.getHora();
    }

    @Transactional
    public Notificacao registrar(Agendamento a, TipoNotificacao tipo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setAgendamento(a);
        n.setTipo(tipo);
        n.setMensagem(mensagem.length() > 400 ? mensagem.substring(0, 397) + "..." : mensagem);
        return repo.save(n);
    }

    private Long filtroPaciente(UsuarioLogado u) {
        return u.is(PACIENTE) ? u.pacienteId() : 0L;
    }

    private Long filtroProfissional(UsuarioLogado u) {
        return u.is(PROFISSIONAL) ? u.profissionalId() : 0L;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(UsuarioLogado u) {
        return repo.listar(filtroPaciente(u), filtroProfissional(u)).stream()
                .limit(100).map(mapeador::notificacao).toList();
    }

    @Transactional(readOnly = true)
    public long naoLidas(UsuarioLogado u) {
        return repo.listar(filtroPaciente(u), filtroProfissional(u)).stream().filter(n -> !n.isLida()).count();
    }

    @Transactional
    public void marcarLida(Long id, UsuarioLogado u) {
        Notificacao n = repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Notificação não encontrada."));
        boolean visivel = repo.listar(filtroPaciente(u), filtroProfissional(u)).stream()
                .anyMatch(x -> x.getId().equals(id));
        if (!visivel) throw new AcessoNegadoException();
        n.setLida(true);
    }

    @Transactional
    public void marcarTodasLidas(UsuarioLogado u) {
        repo.listar(filtroPaciente(u), filtroProfissional(u)).forEach(n -> n.setLida(true));
    }

    /** Simula o envio de lembretes para as consultas que acontecem daqui a "dias" dias. */
    @Transactional
    public int gerarLembretes(int dias, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR, RECEPCIONISTA);
        LocalDate alvo = LocalDate.now().plusDays(Math.max(0, dias));
        int criados = 0;
        for (Agendamento a : agendamentos.findByDataAndStatusIn(alvo,
                List.of(StatusAgendamento.AGENDADA, StatusAgendamento.CONFIRMADA))) {
            if (repo.existsByAgendamentoIdAndTipo(a.getId(), TipoNotificacao.LEMBRETE)) continue;
            registrar(a, TipoNotificacao.LEMBRETE, "Lembrete para " + a.getPaciente().getNome()
                    + ": você tem consulta com " + descricao(a) + ". Em caso de imprevisto, cancele ou remarque com antecedência.");
            criados++;
        }
        return criados;
    }
}
