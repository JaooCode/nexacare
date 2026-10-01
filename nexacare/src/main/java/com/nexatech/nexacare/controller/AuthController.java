package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.AuthDtos.*;
import com.nexatech.nexacare.dto.MensagemResponse;
import com.nexatech.nexacare.security.AuthInterceptor;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AutenticacaoService service;

    public AuthController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return service.login(r);
    }

    @PostMapping("/logout")
    public MensagemResponse logout(@RequestAttribute(AuthInterceptor.ATRIBUTO_TOKEN) String token) {
        service.logout(token);
        return new MensagemResponse("Sessão encerrada.");
    }

    @GetMapping("/me")
    public UsuarioResponse me(UsuarioLogado usuario) {
        return service.me(usuario);
    }

    @PostMapping("/registrar-paciente")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroPacienteRequest r) {
        return service.registrarPaciente(r);
    }

    @PostMapping("/recuperar-senha")
    public MensagemResponse recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest r) {
        return new MensagemResponse(service.recuperarSenha(r.email()));
    }

    @PutMapping("/senha")
    public MensagemResponse alterarSenha(UsuarioLogado usuario, @Valid @RequestBody AlterarSenhaRequest r) {
        service.alterarSenha(usuario, r);
        return new MensagemResponse("Senha alterada com sucesso.");
    }

    @PutMapping("/preferencias")
    public UsuarioResponse preferencias(UsuarioLogado usuario, @Valid @RequestBody PreferenciasRequest r) {
        return service.atualizarPreferencias(usuario, r);
    }
}
