package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.AgendaDtos.NotificacaoResponse;
import com.nexatech.nexacare.dto.MensagemResponse;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.NotificacaoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {
    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoResponse> listar(UsuarioLogado usuario) {
        return service.listar(usuario);
    }

    @PatchMapping("/{id}/lida")
    public MensagemResponse marcarLida(UsuarioLogado usuario, @PathVariable Long id) {
        service.marcarLida(id, usuario);
        return new MensagemResponse("Notificação marcada como lida.");
    }

    @PostMapping("/lidas")
    public MensagemResponse marcarTodasLidas(UsuarioLogado usuario) {
        service.marcarTodasLidas(usuario);
        return new MensagemResponse("Todas as notificações foram marcadas como lidas.");
    }

    /** Simulação do envio automático de lembretes (véspera da consulta). */
    @PostMapping("/lembretes")
    public MensagemResponse gerarLembretes(UsuarioLogado usuario, @RequestParam(defaultValue = "1") int dias) {
        int n = service.gerarLembretes(dias, usuario);
        return new MensagemResponse(n == 0
                ? "Nenhuma consulta pendente de lembrete para o período."
                : n + (n == 1 ? " lembrete gerado" : " lembretes gerados") + " com sucesso (simulação).");
    }
}
