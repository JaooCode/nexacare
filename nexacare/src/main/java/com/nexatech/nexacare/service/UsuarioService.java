package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AuthDtos.UsuarioNovoRequest;
import com.nexatech.nexacare.dto.AuthDtos.UsuarioResponse;
import com.nexatech.nexacare.exception.ExcecoesNegocio.ConflitoException;
import com.nexatech.nexacare.exception.ExcecoesNegocio.NaoEncontradoException;
import com.nexatech.nexacare.exception.ExcecoesNegocio.RegraNegocioException;
import com.nexatech.nexacare.model.Perfil;
import com.nexatech.nexacare.model.Usuario;
import com.nexatech.nexacare.repository.UsuarioRepository;
import com.nexatech.nexacare.security.TokenStore;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.nexatech.nexacare.model.Perfil.ADMINISTRADOR;
import static com.nexatech.nexacare.model.Perfil.RECEPCIONISTA;

/** Gestão de usuários e permissões (somente administrador). */
@Service
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final TokenStore tokens;
    private final Mapeador mapeador;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, TokenStore tokens, Mapeador mapeador) {
        this.repo = repo;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UsuarioLogado u) {
        u.exigir(ADMINISTRADOR);
        return repo.findAllByOrderByNomeAsc().stream().map(mapeador::usuario).toList();
    }

    @Transactional
    public UsuarioResponse criar(UsuarioNovoRequest r, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR);
        if (r.perfil() != ADMINISTRADOR && r.perfil() != RECEPCIONISTA) {
            throw new RegraNegocioException(
                    "Aqui só é possível criar administradores e recepcionistas. "
                            + "Profissionais e pacientes são criados em seus próprios cadastros.");
        }
        String email = Validacoes.validarEmail(r.email());
        Validacoes.validarSenha(r.senha());
        if (repo.existsByEmailIgnoreCase(email)) throw new ConflitoException("Já existe um usuário com este e-mail.");
        Usuario novo = new Usuario();
        novo.setNome(r.nome().trim());
        novo.setEmail(email);
        novo.setSenhaHash(encoder.encode(r.senha()));
        novo.setPerfil(r.perfil());
        return mapeador.usuario(repo.save(novo));
    }

    @Transactional
    public UsuarioResponse alterarAtivo(Long id, boolean ativo, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR);
        Usuario alvo = obter(id);
        if (alvo.getId().equals(u.id())) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        alvo.setAtivo(ativo);
        if (!ativo) tokens.removerTodasDe(id);
        return mapeador.usuario(alvo);
    }

    @Transactional
    public UsuarioResponse alterarPerfil(Long id, Perfil perfil, UsuarioLogado u) {
        u.exigir(ADMINISTRADOR);
        Usuario alvo = obter(id);
        if (alvo.getId().equals(u.id())) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (alvo.getProfissional() != null || alvo.getPaciente() != null) {
            throw new RegraNegocioException("O perfil deste usuário está vinculado ao seu cadastro e não pode ser alterado.");
        }
        if (perfil != ADMINISTRADOR && perfil != RECEPCIONISTA) {
            throw new RegraNegocioException("Perfil não permitido para este usuário.");
        }
        alvo.setPerfil(perfil);
        tokens.removerTodasDe(id);
        return mapeador.usuario(alvo);
    }

    private Usuario obter(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }
}
