package com.nexatech.nexacare.security;

import com.nexatech.nexacare.exception.ExcecoesNegocio.NaoAutenticadoException;
import com.nexatech.nexacare.model.Usuario;
import com.nexatech.nexacare.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Protege todas as rotas /api/** (exceto as públicas) exigindo "Authorization: Bearer <token>". */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String ATRIBUTO_USUARIO = "usuarioLogado";
    public static final String ATRIBUTO_TOKEN = "tokenAtual";

    private final TokenStore tokens;
    private final UsuarioRepository usuarios;

    public AuthInterceptor(TokenStore tokens, UsuarioRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if ("OPTIONS".equals(req.getMethod())) return true;

        String header = req.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        Long usuarioId = tokens.usuarioDe(token);
        Usuario usuario = usuarioId == null ? null : usuarios.findById(usuarioId).orElse(null);

        if (usuario == null || !usuario.isAtivo()) {
            throw new NaoAutenticadoException("Sessão expirada. Faça login novamente.");
        }
        req.setAttribute(ATRIBUTO_USUARIO, UsuarioLogado.de(usuario));
        req.setAttribute(ATRIBUTO_TOKEN, token);
        return true;
    }
}
