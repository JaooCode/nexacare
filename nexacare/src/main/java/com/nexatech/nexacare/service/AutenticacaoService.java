package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AuthDtos.*;
import com.nexatech.nexacare.dto.CadastroDtos.PacienteRequest;
import com.nexatech.nexacare.exception.ExcecoesNegocio.NaoAutenticadoException;
import com.nexatech.nexacare.exception.ExcecoesNegocio.RegraNegocioException;
import com.nexatech.nexacare.model.Paciente;
import com.nexatech.nexacare.model.Perfil;
import com.nexatech.nexacare.model.Usuario;
import com.nexatech.nexacare.repository.PacienteRepository;
import com.nexatech.nexacare.repository.UsuarioRepository;
import com.nexatech.nexacare.security.TokenStore;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutenticacaoService {
    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);
    private static final int MAX_TENTATIVAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(5);
    private static final String MSG_LOGIN_INVALIDO = "E-mail ou senha inválidos.";

    private static final class Tentativas {
        int falhas;
        Instant bloqueadoAte;
    }

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final PacienteService pacienteService;
    private final PasswordEncoder encoder;
    private final TokenStore tokens;
    private final Mapeador mapeador;

    public AutenticacaoService(UsuarioRepository usuarios, PacienteRepository pacientes,
                               PacienteService pacienteService, PasswordEncoder encoder, TokenStore tokens,
                               Mapeador mapeador) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.pacienteService = pacienteService;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mapeador = mapeador;
    }

    /** Mesma mensagem para e-mail inexistente, senha errada ou usuário inativo (não revela qual dado falhou). */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest r) {
        String email = Validacoes.normalizarEmail(r.email());
        Tentativas t = tentativas.computeIfAbsent(email, k -> new Tentativas());
        if (t.bloqueadoAte != null && t.bloqueadoAte.isAfter(Instant.now())) {
            throw new NaoAutenticadoException("Muitas tentativas de acesso. Aguarde alguns minutos e tente novamente.");
        }
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElse(null);
        boolean ok = u != null && u.isAtivo() && encoder.matches(r.senha(), u.getSenhaHash());
        if (!ok) {
            if (++t.falhas >= MAX_TENTATIVAS) {
                t.bloqueadoAte = Instant.now().plus(BLOQUEIO);
                t.falhas = 0;
            }
            throw new NaoAutenticadoException(MSG_LOGIN_INVALIDO);
        }
        tentativas.remove(email);
        return new LoginResponse(tokens.criar(u.getId()), mapeador.usuario(u));
    }

    public void logout(String token) {
        tokens.remover(token);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(UsuarioLogado u) {
        return mapeador.usuario(usuarios.findById(u.id()).orElseThrow());
    }

    /** Autocadastro: cria o cadastro mínimo do paciente e o acesso dele, tudo na mesma transação. */
    @Transactional
    public UsuarioResponse registrarPaciente(RegistroPacienteRequest r) {
        final String mensagem = "Não foi possível concluir o cadastro. Verifique os dados informados "
                + "ou procure a recepção da clínica.";
        Validacoes.validarSenha(r.senha());
        String email = Validacoes.validarEmail(r.email());
        String cpf = Validacoes.validarCpf(r.cpf());
        if (usuarios.existsByEmailIgnoreCase(email) || pacientes.existsByCpf(cpf)) {
            throw new RegraNegocioException(mensagem);
        }
        Paciente p = pacienteService.criarInterno(
                new PacienteRequest(r.nome(), r.cpf(), r.dataNascimento(), r.telefone(), email, null));
        Usuario u = new Usuario();
        u.setNome(p.getNome());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(r.senha()));
        u.setPerfil(Perfil.PACIENTE);
        u.setPaciente(p);
        return mapeador.usuario(usuarios.save(u));
    }

    /** Simulação: não envia e-mail e responde igual exista ou não a conta (evita enumeração de usuários). */
    public String recuperarSenha(String email) {
        log.info("Solicitação de recuperação de senha (simulada) recebida.");
        return "Se o e-mail informado estiver cadastrado, você receberá as instruções de recuperação. "
                + "(Simulação: nenhum e-mail real é enviado. Procure a recepção para redefinir sua senha.)";
    }

    @Transactional
    public void alterarSenha(UsuarioLogado logado, AlterarSenhaRequest r) {
        Usuario u = usuarios.findById(logado.id()).orElseThrow();
        if (!encoder.matches(r.senhaAtual(), u.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        Validacoes.validarSenha(r.novaSenha());
        if (encoder.matches(r.novaSenha(), u.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        u.setSenhaHash(encoder.encode(r.novaSenha()));
    }

    @Transactional
    public UsuarioResponse atualizarPreferencias(UsuarioLogado logado, PreferenciasRequest r) {
        Usuario u = usuarios.findById(logado.id()).orElseThrow();
        u.setNome(r.nome().trim());
        u.setNotificacoesAtivas(r.notificacoesAtivas());
        u.setTema(r.tema());
        return mapeador.usuario(u);
    }
}
