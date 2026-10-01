package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.CadastroDtos.*;
import com.nexatech.nexacare.exception.ExcecoesNegocio.*;
import com.nexatech.nexacare.model.*;
import com.nexatech.nexacare.repository.*;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.security.TokenStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.nexatech.nexacare.model.Perfil.ADMINISTRADOR;
import static com.nexatech.nexacare.model.Perfil.RECEPCIONISTA;

@Service
public class ProfissionalService {
    private static final List<StatusAgendamento> ATIVAS =
            Arrays.stream(StatusAgendamento.values()).filter(StatusAgendamento::isAtiva).toList();

    private final ProfissionalRepository repo;
    private final EspecialidadeRepository especialidades;
    private final UsuarioRepository usuarios;
    private final AgendamentoRepository agendamentos;
    private final PasswordEncoder encoder;
    private final TokenStore tokens;

    public ProfissionalService(ProfissionalRepository repo, EspecialidadeRepository especialidades,
                               UsuarioRepository usuarios, AgendamentoRepository agendamentos,
                               PasswordEncoder encoder, TokenStore tokens) {
        this.repo = repo;
        this.especialidades = especialidades;
        this.usuarios = usuarios;
        this.agendamentos = agendamentos;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    /** Contato (telefone/e-mail) só aparece para quem gerencia profissionais. */
    private ProfissionalResponse resposta(Profissional p, UsuarioLogado u) {
        boolean gestor = u.is(ADMINISTRADOR, RECEPCIONISTA);
        return new ProfissionalResponse(p.getId(), p.getNome(), p.getEspecialidade().getId(),
                p.getEspecialidade().getNome(), p.getRegistro(), gestor ? p.getTelefone() : null,
                gestor ? p.getEmail() : null, p.isAtivo(),
                gestor && usuarios.findByProfissionalId(p.getId()).isPresent());
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponse> listar(String busca, Long especialidadeId, boolean incluirInativos,
                                             UsuarioLogado u) {
        boolean todos = incluirInativos && u.is(ADMINISTRADOR, RECEPCIONISTA);
        return repo.pesquisar(Validacoes.padraoBusca(busca), especialidadeId == null ? 0L : especialidadeId, todos)
                .stream().map(p -> resposta(p, u)).toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalResponse buscar(Long id, UsuarioLogado u) {
        return resposta(obter(id), u);
    }

    @Transactional
    public ProfissionalResponse criar(ProfissionalRequest r, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR, RECEPCIONISTA);
        Profissional p = new Profissional();
        preencher(p, r);
        p.setAtivo(r.ativo());
        repo.save(p);

        if (r.senhaInicial() != null && !r.senhaInicial().isBlank()) {
            Validacoes.validarSenha(r.senhaInicial());
            if (usuarios.existsByEmailIgnoreCase(p.getEmail())) {
                throw new ConflitoException("Já existe um usuário com este e-mail.");
            }
            Usuario acesso = new Usuario();
            acesso.setNome(p.getNome());
            acesso.setEmail(p.getEmail());
            acesso.setSenhaHash(encoder.encode(r.senhaInicial()));
            acesso.setPerfil(Perfil.PROFISSIONAL);
            acesso.setProfissional(p);
            acesso.setAtivo(p.isAtivo());
            usuarios.save(acesso);
        }
        return resposta(p, u);
    }

    @Transactional
    public ProfissionalResponse atualizar(Long id, ProfissionalRequest r, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR, RECEPCIONISTA);
        Profissional p = obter(id);
        preencher(p, r);
        usuarios.findByProfissionalId(id).ifPresent(acesso -> {
            acesso.setNome(p.getNome());
            if (!acesso.getEmail().equalsIgnoreCase(p.getEmail())) {
                if (usuarios.existsByEmailIgnoreCase(p.getEmail())) {
                    throw new ConflitoException("Já existe um usuário com este e-mail.");
                }
                acesso.setEmail(p.getEmail());
            }
        });
        return resposta(p, u);
    }

    @Transactional
    public ProfissionalResponse alterarStatus(Long id, boolean ativo, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR, RECEPCIONISTA);
        Profissional p = obter(id);
        if (!ativo && agendamentos.futurasDoProfissional(id, ATIVAS, LocalDate.now(), LocalTime.now()) > 0) {
            throw new RegraNegocioException(
                    "Este profissional possui consultas futuras. Cancele ou remarque antes de desativar.");
        }
        p.setAtivo(ativo);
        usuarios.findByProfissionalId(id).ifPresent(acesso -> {
            acesso.setAtivo(ativo);
            if (!ativo) tokens.removerTodasDe(acesso.getId());
        });
        return resposta(p, u);
    }

    private Profissional obter(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Profissional não encontrado."));
    }

    private void preencher(Profissional p, ProfissionalRequest r) {
        Especialidade e = especialidades.findById(r.especialidadeId())
                .orElseThrow(() -> new RegraNegocioException("Especialidade inválida."));
        p.setNome(r.nome().trim());
        p.setEspecialidade(e);
        p.setRegistro(r.registro() == null || r.registro().isBlank() ? null : r.registro().trim());
        p.setTelefone(Validacoes.validarTelefone(r.telefone()));
        p.setEmail(Validacoes.validarEmail(r.email()));
    }

    // ---- Especialidades ----

    @Transactional(readOnly = true)
    public List<EspecialidadeResponse> listarEspecialidades() {
        return especialidades.findAllByOrderByNomeAsc().stream()
                .map(e -> new EspecialidadeResponse(e.getId(), e.getNome())).toList();
    }

    @Transactional
    public EspecialidadeResponse criarEspecialidade(EspecialidadeRequest r, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR);
        String nome = r.nome().trim();
        if (especialidades.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Esta especialidade já está cadastrada.");
        }
        Especialidade e = new Especialidade();
        e.setNome(nome);
        especialidades.save(e);
        return new EspecialidadeResponse(e.getId(), e.getNome());
    }
}
