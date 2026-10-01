package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.CadastroDtos.PacienteRequest;
import com.nexatech.nexacare.dto.CadastroDtos.PacienteResponse;
import com.nexatech.nexacare.exception.ExcecoesNegocio.*;
import com.nexatech.nexacare.model.Paciente;
import com.nexatech.nexacare.model.StatusAgendamento;
import com.nexatech.nexacare.repository.AgendamentoRepository;
import com.nexatech.nexacare.repository.PacienteRepository;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.nexatech.nexacare.model.Perfil.PROFISSIONAL;
import static com.nexatech.nexacare.model.Perfil.RECEPCIONISTA;

@Service
public class PacienteService {
    private static final List<StatusAgendamento> ATIVAS =
            Arrays.stream(StatusAgendamento.values()).filter(StatusAgendamento::isAtiva).toList();

    private final PacienteRepository repo;
    private final AgendamentoRepository agendamentos;

    public PacienteService(PacienteRepository repo, AgendamentoRepository agendamentos) {
        this.repo = repo;
        this.agendamentos = agendamentos;
    }

    /**
     * Minimização de dados: a recepção vê o cadastro completo; o profissional vê apenas o necessário
     * ao atendimento (sem CPF e sem e-mail).
     */
    private PacienteResponse resposta(Paciente p, UsuarioLogado u) {
        boolean completo = u.is(RECEPCIONISTA);
        return new PacienteResponse(p.getId(), p.getNome(), completo ? Validacoes.formatarCpf(p.getCpf()) : null,
                p.getDataNascimento(), p.getTelefone(), completo ? p.getEmail() : null, p.getObservacao(), p.isAtivo());
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> listar(String busca, boolean incluirInativos, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA, PROFISSIONAL);
        String termo = Validacoes.padraoBusca(busca);
        String termoCpf = Validacoes.padraoBuscaCpf(busca);
        List<Paciente> lista = u.is(PROFISSIONAL)
                ? repo.pesquisarDoProfissional(u.profissionalId(), termo, termoCpf)
                : repo.pesquisar(termo, termoCpf, incluirInativos);
        return lista.stream().map(p -> resposta(p, u)).toList();
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscar(Long id, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA, PROFISSIONAL);
        Paciente p = obter(id);
        if (u.is(PROFISSIONAL) && !agendamentos.existsByProfissionalIdAndPacienteId(u.profissionalId(), id)) {
            throw new AcessoNegadoException();
        }
        return resposta(p, u);
    }

    @Transactional
    public PacienteResponse criar(PacienteRequest r, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA);
        Paciente p = new Paciente();
        preencher(p, r, null);
        return resposta(repo.save(p), u);
    }

    /** Usado também pelo autocadastro do paciente (sem usuário logado). */
    @Transactional
    public Paciente criarInterno(PacienteRequest r) {
        Paciente p = new Paciente();
        preencher(p, r, null);
        return repo.save(p);
    }

    @Transactional
    public PacienteResponse atualizar(Long id, PacienteRequest r, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA);
        Paciente p = obter(id);
        preencher(p, r, id);
        return resposta(p, u);
    }

    @Transactional
    public PacienteResponse alterarStatus(Long id, boolean ativo, UsuarioLogado u) {
        u.exigir(RECEPCIONISTA);
        Paciente p = obter(id);
        if (!ativo && agendamentos.futurasDoPaciente(id, ATIVAS, LocalDate.now(), LocalTime.now()) > 0) {
            throw new RegraNegocioException(
                    "Este paciente possui consultas futuras. Cancele-as antes de desativar o cadastro.");
        }
        p.setAtivo(ativo);
        return resposta(p, u);
    }

    private Paciente obter(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado."));
    }

    private void preencher(Paciente p, PacienteRequest r, Long idAtual) {
        String cpf = Validacoes.validarCpf(r.cpf());
        boolean duplicado = idAtual == null ? repo.existsByCpf(cpf) : repo.existsByCpfAndIdNot(cpf, idAtual);
        if (duplicado) throw new ConflitoException("Já existe um paciente cadastrado com este CPF.");
        if (r.dataNascimento().isBefore(LocalDate.of(1900, 1, 1))) {
            throw new RegraNegocioException("Data de nascimento inválida.");
        }
        p.setNome(r.nome().trim());
        p.setCpf(cpf);
        p.setDataNascimento(r.dataNascimento());
        p.setTelefone(Validacoes.validarTelefone(r.telefone()));
        p.setEmail(Validacoes.validarEmail(r.email()));
        p.setObservacao(r.observacao() == null || r.observacao().isBlank() ? null : r.observacao().trim());
    }
}
