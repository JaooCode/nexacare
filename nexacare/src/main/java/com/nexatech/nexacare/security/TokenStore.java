package com.nexatech.nexacare.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sessões em memória: token aleatório -> id do usuário. Reiniciar o servidor encerra as sessões. */
@Component
public class TokenStore {
    private static final Duration VALIDADE = Duration.ofHours(8);

    private record Sessao(Long usuarioId, Instant expira) {}

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String criar(Long usuarioId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuarioId, Instant.now().plus(VALIDADE)));
        return token;
    }

    /** Retorna o id do usuário dono do token, ou null se inexistente/expirado. */
    public Long usuarioDe(String token) {
        Sessao s = token == null ? null : sessoes.get(token);
        if (s == null) return null;
        if (s.expira().isBefore(Instant.now())) {
            sessoes.remove(token);
            return null;
        }
        return s.usuarioId();
    }

    public void remover(String token) {
        if (token != null) sessoes.remove(token);
    }

    public void removerTodasDe(Long usuarioId) {
        sessoes.values().removeIf(s -> s.usuarioId().equals(usuarioId));
    }
}
