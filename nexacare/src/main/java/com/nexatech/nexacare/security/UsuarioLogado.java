package com.nexatech.nexacare.security;

import com.nexatech.nexacare.exception.ExcecoesNegocio.AcessoNegadoException;
import com.nexatech.nexacare.model.Perfil;
import com.nexatech.nexacare.model.Usuario;

import java.util.Arrays;

/** Usuário autenticado da requisição atual (montado a cada requisição a partir do banco). */
public record UsuarioLogado(Long id, String nome, String email, Perfil perfil,
                            Long profissionalId, Long pacienteId) {

    public static UsuarioLogado de(Usuario u) {
        return new UsuarioLogado(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getProfissional() != null ? u.getProfissional().getId() : null,
                u.getPaciente() != null ? u.getPaciente().getId() : null);
    }

    public boolean is(Perfil... perfis) {
        return Arrays.asList(perfis).contains(perfil);
    }

    /** Lança 403 se o perfil não estiver entre os permitidos. */
    public void exigir(Perfil... perfis) {
        if (!is(perfis)) {
            throw new AcessoNegadoException();
        }
    }
}
